package dinhlam2901.sunilies.controller;

import dinhlam2901.sunilies.model.CartItem;
import dinhlam2901.sunilies.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    // ══════════════════════════════════════════════════════
    // TRANG CART (GET /cart)
    // ══════════════════════════════════════════════════════
    @GetMapping("/cart")
    public String cartPage(HttpSession session, Model model) {
        List<CartItem> items = cartService.getCart(session);
        double total = cartService.getTotalPrice(items);
        int    qty   = cartService.getTotalQty(items);

        model.addAttribute("cartItems",  items);
        model.addAttribute("totalPrice", total);
        model.addAttribute("totalQty",   qty);
        return "cart";
    }

    // ══════════════════════════════════════════════════════
    // REST API
    // ══════════════════════════════════════════════════════

    /** GET /api/cart — lấy giỏ hàng hiện tại */
    @GetMapping("/api/cart")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCart(HttpSession session) {
        List<CartItem> items = cartService.getCart(session);
        return ResponseEntity.ok(buildResponse(items));
    }

    /** POST /api/cart/add — thêm sản phẩm */
    @PostMapping("/api/cart/add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addToCart(
            @RequestBody CartItem item,
            HttpSession session) {
        List<CartItem> items = cartService.addItem(session, item);
        return ResponseEntity.ok(buildResponse(items));
    }

    /** PUT /api/cart/{key}?qty=N — cập nhật số lượng */
    @PutMapping("/api/cart/{key}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateQty(
            @PathVariable String key,
            @RequestParam int qty,
            HttpSession session) {
        List<CartItem> items = cartService.updateQty(session, key, qty);
        return ResponseEntity.ok(buildResponse(items));
    }

    /** DELETE /api/cart/{key} — xoá item */
    @DeleteMapping("/api/cart/{key}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeItem(
            @PathVariable String key,
            HttpSession session) {
        List<CartItem> items = cartService.removeItem(session, key);
        return ResponseEntity.ok(buildResponse(items));
    }

    /** DELETE /api/cart — xoá toàn bộ */
    @DeleteMapping("/api/cart")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> clearCart(HttpSession session) {
        cartService.clearCart(session);
        return ResponseEntity.ok(buildResponse(List.of()));
    }

    // ── Helper ────────────────────────────────────────────
    private Map<String, Object> buildResponse(List<CartItem> items) {
        return Map.of(
                "items",      items,
                "totalPrice", cartService.getTotalPrice(items),
                "totalQty",   cartService.getTotalQty(items),
                "count",      items.size()
        );
    }
}