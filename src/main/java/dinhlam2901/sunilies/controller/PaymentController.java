package dinhlam2901.sunilies.controller;

import dinhlam2901.sunilies.model.CartItem;
import dinhlam2901.sunilies.model.Order;
import dinhlam2901.sunilies.repository.OrderRepository;
import dinhlam2901.sunilies.service.CartService;
import dinhlam2901.sunilies.service.MomoPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class PaymentController {

    @Autowired private MomoPaymentService momoService;
    @Autowired private OrderRepository    orderRepository;
    @Autowired private CartService        cartService;

    // ══════════════════════════════════════════════════════
    // 1. TẠNG CHECKOUT
    // ══════════════════════════════════════════════════════
    @GetMapping("/checkout")
    public String checkoutPage(HttpSession session, Model model) {
        List<CartItem> items = cartService.getCart(session);
        if (items.isEmpty()) return "redirect:/cart";

        model.addAttribute("cartItems",  items);
        model.addAttribute("totalPrice", cartService.getTotalPrice(items));
        return "checkout";
    }

    // ══════════════════════════════════════════════════════
    // 2. TẠO ĐƠN HÀNG + GỌI MOMO API → trả payUrl
    //    POST /api/payment/momo
    //    Body: { fullName, phone, address, note }
    // ══════════════════════════════════════════════════════
    @PostMapping("/api/payment/momo")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createMomoPayment(
            @RequestBody Map<String, String> body,
            HttpSession session,
            HttpServletRequest request) {

        try {
            List<CartItem> items = cartService.getCart(session);
            if (items.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Giỏ hàng trống"));
            }

            double total = cartService.getTotalPrice(items);
            long   amount = Math.round(total); // VND, không có xu

            // ── Tạo đơn hàng ─────────────────────────────
            String orderId = "ORD_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

            Order order = new Order();
            order.setId(orderId);
            order.setItems(items);
            order.setTotalAmount(total);
            order.setFullName(body.get("fullName"));
            order.setPhone(body.get("phone"));
            order.setAddress(body.get("address"));
            order.setNote(body.getOrDefault("note", ""));
            order.setStatus("PENDING");
            order.setPaymentMethod("MOMO");
            order.setCreatedAt(System.currentTimeMillis());
            order.setUpdatedAt(System.currentTimeMillis());

            // Gắn userId nếu đã đăng nhập
            Object userId = session.getAttribute("userId");
            if (userId != null) order.setUserId(userId.toString());

            // ── Lưu đơn vào Firestore (status PENDING) ───
            orderRepository.save(order);

            // ── Gọi MoMo tạo payUrl ───────────────────────
            String orderInfo = "Thanh toan don hang " + orderId;
            String payUrl = momoService.createPayment(orderId, amount, orderInfo);

            if (payUrl == null || payUrl.isEmpty()) {
                return ResponseEntity.internalServerError()
                        .body(Map.of("error", "MoMo không trả về payUrl"));
            }

            return ResponseEntity.ok(Map.of(
                    "payUrl",  payUrl,
                    "orderId", orderId
            ));

        } catch (Exception e) {
            System.err.println("❌ Lỗi tạo MoMo payment: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════
    // 3. RETURN URL — MoMo redirect browser về đây
    //    GET /payment/momo/return?orderId=...&resultCode=0&...
    //    CHỈ hiển thị kết quả, KHÔNG cập nhật DB
    // ══════════════════════════════════════════════════════
    @GetMapping("/payment/momo/return")
    public String momoReturn(
            @RequestParam String  orderId,
            @RequestParam int     resultCode,
            @RequestParam String  message,
            @RequestParam(defaultValue = "") String requestId,
            @RequestParam(defaultValue = "") String orderInfo,
            @RequestParam(defaultValue = "") String orderType,
            @RequestParam(defaultValue = "0") long  transId,
            @RequestParam(defaultValue = "") String payType,
            @RequestParam(defaultValue = "0") long  responseTime,
            @RequestParam(defaultValue = "") String extraData,
            @RequestParam(defaultValue = "") String signature,
            @RequestParam(defaultValue = "0") long  amount,   // ← thêm dòng này
            Model model) {

        boolean success = (resultCode == 0);

        try {
            boolean valid = momoService.verifySignature(
                    signature, requestId, orderId,
                    amount,        // ← truyền amount thật thay vì 0
                    orderInfo, orderType, transId, resultCode,
                    message, payType, responseTime, extraData);

            if (!valid) {
                model.addAttribute("success", false);
                model.addAttribute("message", "Chữ ký không hợp lệ");
                return "payment-result";
            }
        } catch (Exception e) {
            System.err.println("❌ Verify return signature lỗi: " + e.getMessage());
        }

        model.addAttribute("success",  success);
        model.addAttribute("orderId",  orderId);
        model.addAttribute("transId",  transId);
        model.addAttribute("message",  message);
        return "payment-result";
    }

    // ══════════════════════════════════════════════════════
    // 4. IPN — MoMo gọi trực tiếp đến server (bất đồng bộ)
    //    POST /payment/momo/ipn
    //    ★ Đây là nơi DUY NHẤT được phép cập nhật DB
    // ══════════════════════════════════════════════════════
    @PostMapping("/payment/momo/ipn")
    @ResponseBody
    public ResponseEntity<Void> momoIpn(@RequestBody Map<String, Object> body) {
        try {
            String orderId      = str(body, "orderId");
            String requestId    = str(body, "requestId");
            String orderInfo    = str(body, "orderInfo");
            String orderType    = str(body, "orderType");
            String payType      = str(body, "payType");
            String message      = str(body, "message");
            String extraData    = str(body, "extraData");
            String signature    = str(body, "signature");
            long   amount       = toLong(body, "amount");
            long   transId      = toLong(body, "transId");
            int    resultCode   = toInt(body,  "resultCode");
            long   responseTime = toLong(body, "responseTime");

            System.out.printf("📩 MoMo IPN: orderId=%s resultCode=%d transId=%d%n",
                    orderId, resultCode, transId);

            // ── Verify signature ──────────────────────────
            boolean valid = momoService.verifySignature(
                    signature, requestId, orderId, amount, orderInfo,
                    orderType, transId, resultCode, message,
                    payType, responseTime, extraData);

            if (!valid) {
                System.err.println("❌ MoMo IPN: chữ ký không hợp lệ, orderId=" + orderId);
                return ResponseEntity.ok().build(); // vẫn 200 để MoMo không retry
            }

            // ── Kiểm tra đơn tồn tại (idempotency) ───────
            Order order = orderRepository.findById(orderId);
            if (order == null) {
                System.err.println("❌ MoMo IPN: không tìm thấy đơn " + orderId);
                return ResponseEntity.ok().build();
            }

            // Tránh xử lý lại đơn đã PAID
            if ("PAID".equals(order.getStatus())) {
                System.out.println("⚠️ MoMo IPN: đơn " + orderId + " đã PAID, bỏ qua");
                return ResponseEntity.ok().build();
            }

            // ── Cập nhật trạng thái ───────────────────────
            String newStatus = (resultCode == 0) ? "PAID" : "FAILED";
            orderRepository.updatePaymentResult(orderId, newStatus, String.valueOf(transId));

            System.out.printf("✅ Đơn %s → %s (transId=%d)%n", orderId, newStatus, transId);

            // Nếu PAID: có thể gửi email xác nhận ở đây
            // if ("PAID".equals(newStatus)) emailService.sendOrderConfirmation(order);

        } catch (Exception e) {
            System.err.println("❌ Lỗi xử lý MoMo IPN: " + e.getMessage());
            // Vẫn trả 200 để MoMo không retry liên tục
        }

        return ResponseEntity.ok().build();
    }

    // ── Helpers parse Map ──────────────────────────────────
    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : "";
    }
    private long toLong(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v == null) return 0L;
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return 0L; }
    }
    private int toInt(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v == null) return -1;
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return -1; }
    }
}