package dinhlam2901.sunilies.controller;

import dinhlam2901.sunilies.model.Product;
import dinhlam2901.sunilies.model.User;
import dinhlam2901.sunilies.repository.ProductRepository;
import dinhlam2901.sunilies.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AdminController – Quản lý trang admin.
 * Chỉ user có role = "ADMIN" mới được truy cập.
 * Tất cả route đều có prefix /admin
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private AuthService       authService;
    @Autowired private ProductRepository productRepository;

    // ══════════════════════════════════════════════════════
    // HELPER: Kiểm tra quyền ADMIN
    // ══════════════════════════════════════════════════════
    private User requireAdmin(HttpSession session) throws Exception {
        if (!authService.isLoggedIn(session)) return null;
        User user = authService.getCurrentUser(session);
        if (user == null || !"ADMIN".equals(user.getRole())) return null;
        return user;
    }

    // ══════════════════════════════════════════════════════
    // 1. DASHBOARD – Tổng quan
    // ══════════════════════════════════════════════════════
    @GetMapping("")
    public String dashboard(HttpSession session, Model model) throws Exception {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/login?redirect=/admin";

        List<Product> products = productRepository.findAll();

        // Thống kê tổng quan
        long totalProducts  = products.size();
        long activeProducts = products.stream().filter(Product::isActive).count();
        long outOfStock     = products.stream().filter(p -> p.getStock() <= 0).count();
        long lowStock       = products.stream().filter(p -> p.getStock() > 0 && p.getStock() <= 5).count();
        double totalValue   = products.stream()
                .mapToDouble(p -> (p.getPrice() != null ? p.getPrice() : 0) * p.getStock())
                .sum();

        model.addAttribute("admin",          admin);
        model.addAttribute("products",       products);
        model.addAttribute("totalProducts",  totalProducts);
        model.addAttribute("activeProducts", activeProducts);
        model.addAttribute("outOfStock",     outOfStock);
        model.addAttribute("lowStock",       lowStock);
        model.addAttribute("totalValue",     totalValue);

        return "admin/dashboard";
    }

    // ══════════════════════════════════════════════════════
    // 2. THÊM SẢN PHẨM MỚI
    // ══════════════════════════════════════════════════════
    @PostMapping("/products/add")
    public String addProduct(
            HttpSession session,
            @RequestParam String  name,
            @RequestParam String  handle,
            @RequestParam String  description,
            @RequestParam(required = false) String fullDescription,
            @RequestParam Double  price,
            @RequestParam(required = false) Double comparePrice,
            @RequestParam String  imageUrl,
            @RequestParam(required = false) String images,       // cách nhau bởi dấu phẩy
            @RequestParam String  category,
            @RequestParam(required = false) String collection,
            @RequestParam(required = false) String sizes,        // "S,M,L,XL"
            @RequestParam(required = false) String colors,       // "Nâu,Kem,Đen"
            @RequestParam(required = false) String tags,         // "new,hot"
            @RequestParam int     stock,
            @RequestParam(required = false) boolean onSale,
            @RequestParam(required = false) boolean isNew,
            @RequestParam(required = false) boolean hot,
            @RequestParam(required = false) boolean active,
            @RequestParam(required = false) String metaTitle,
            @RequestParam(required = false) String metaDescription,
            RedirectAttributes ra) throws Exception {

        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/login?redirect=/admin";

        Product p = new Product();
        p.setName(name.trim());
        p.setHandle(handle.trim().toLowerCase().replaceAll("\\s+", "-"));
        p.setDescription(description.trim());
        p.setFullDescription(fullDescription);
        p.setPrice(price);
        p.setComparePrice(comparePrice);
        p.setImageUrl(imageUrl.trim());
        p.setCategory(category.trim());
        p.setCollection(collection != null ? collection.trim() : null);
        p.setStock(stock);
        p.setOnSale(onSale);
        p.setNew(isNew);
        p.setHot(hot);
        p.setActive(active);
        p.setMetaTitle(metaTitle);
        p.setMetaDescription(metaDescription);

        // Parse danh sách ảnh
        if (images != null && !images.isBlank()) {
            p.setImages(Arrays.stream(images.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .collect(Collectors.toList()));
        }
        // Parse sizes
        if (sizes != null && !sizes.isBlank()) {
            p.setSizes(Arrays.stream(sizes.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .collect(Collectors.toList()));
        }
        // Parse colors
        if (colors != null && !colors.isBlank()) {
            p.setColors(Arrays.stream(colors.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .collect(Collectors.toList()));
        }
        // Parse tags
        if (tags != null && !tags.isBlank()) {
            p.setTags(Arrays.stream(tags.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .collect(Collectors.toList()));
        }

        try {
            productRepository.save(p);
            ra.addFlashAttribute("success", "✅ Thêm sản phẩm \"" + name + "\" thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Lỗi: " + e.getMessage());
        }
        return "redirect:/admin";
    }

    // ══════════════════════════════════════════════════════
    // 3. CẬP NHẬT SẢN PHẨM (full update)
    // ══════════════════════════════════════════════════════
    @PostMapping("/products/{id}/update")
    public String updateProduct(
            HttpSession session,
            @PathVariable String id,
            @RequestParam String  name,
            @RequestParam String  handle,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String fullDescription,
            @RequestParam Double  price,
            @RequestParam(required = false) Double comparePrice,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) String images,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String collection,
            @RequestParam(required = false) String sizes,
            @RequestParam(required = false) String colors,
            @RequestParam(required = false) String tags,
            @RequestParam int     stock,
            @RequestParam(required = false) boolean onSale,
            @RequestParam(required = false) boolean isNew,
            @RequestParam(required = false) boolean hot,
            @RequestParam(required = false) boolean active,
            @RequestParam(required = false) String metaTitle,
            @RequestParam(required = false) String metaDescription,
            RedirectAttributes ra) throws Exception {

        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/login?redirect=/admin";

        try {
            Product p = productRepository.findById(id);
            if (p == null) { ra.addFlashAttribute("error", "Không tìm thấy sản phẩm!"); return "redirect:/admin"; }

            p.setName(name.trim());
            p.setHandle(handle.trim().toLowerCase().replaceAll("\\s+", "-"));
            p.setDescription(description);
            p.setFullDescription(fullDescription);
            p.setPrice(price);
            p.setComparePrice(comparePrice);
            if (imageUrl != null && !imageUrl.isBlank()) p.setImageUrl(imageUrl.trim());
            p.setCategory(category);
            p.setCollection(collection);
            p.setStock(stock);
            p.setOnSale(onSale);
            p.setNew(isNew);
            p.setHot(hot);
            p.setActive(active);
            p.setMetaTitle(metaTitle);
            p.setMetaDescription(metaDescription);

            if (images != null && !images.isBlank()) {
                p.setImages(Arrays.stream(images.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
            }
            if (sizes != null && !sizes.isBlank()) {
                p.setSizes(Arrays.stream(sizes.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
            }
            if (colors != null && !colors.isBlank()) {
                p.setColors(Arrays.stream(colors.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
            }
            if (tags != null && !tags.isBlank()) {
                p.setTags(Arrays.stream(tags.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
            }

            productRepository.update(p);
            ra.addFlashAttribute("success", "✅ Cập nhật \"" + name + "\" thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Lỗi: " + e.getMessage());
        }
        return "redirect:/admin";
    }

    // ══════════════════════════════════════════════════════
    // 4. CẬP NHẬT GIÁ NHANH (AJAX)
    // ══════════════════════════════════════════════════════
    @PostMapping("/products/{id}/price")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updatePrice(
            HttpSession session,
            @PathVariable String id,
            @RequestBody Map<String, Double> body) throws Exception {

        User admin = requireAdmin(session);
        if (admin == null) return ResponseEntity.status(403).body(Map.of("error", "Không có quyền"));

        try {
            Product p = productRepository.findById(id);
            if (p == null) return ResponseEntity.badRequest().body(Map.of("error", "Sản phẩm không tồn tại"));

            if (body.containsKey("price"))        p.setPrice(body.get("price"));
            if (body.containsKey("comparePrice")) p.setComparePrice(body.get("comparePrice"));
            if (p.getComparePrice() != null && p.getComparePrice() > p.getPrice()) p.setOnSale(true);

            productRepository.update(p);
            return ResponseEntity.ok(Map.of("success", true, "price", p.getPrice(), "comparePrice", p.getComparePrice() != null ? p.getComparePrice() : 0));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════
    // 5. CẬP NHẬT TỒN KHO NHANH (AJAX)
    // ══════════════════════════════════════════════════════
    @PostMapping("/products/{id}/stock")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateStock(
            HttpSession session,
            @PathVariable String id,
            @RequestBody Map<String, Integer> body) throws Exception {

        User admin = requireAdmin(session);
        if (admin == null) return ResponseEntity.status(403).body(Map.of("error", "Không có quyền"));

        try {
            Product p = productRepository.findById(id);
            if (p == null) return ResponseEntity.badRequest().body(Map.of("error", "Sản phẩm không tồn tại"));

            int newStock = body.getOrDefault("stock", p.getStock());
            p.setStock(newStock);
            productRepository.update(p);
            return ResponseEntity.ok(Map.of("success", true, "stock", newStock));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════
    // 6. BẬT / TẮT ACTIVE (AJAX)
    // ══════════════════════════════════════════════════════
    @PostMapping("/products/{id}/toggle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleActive(
            HttpSession session,
            @PathVariable String id) throws Exception {

        User admin = requireAdmin(session);
        if (admin == null) return ResponseEntity.status(403).body(Map.of("error", "Không có quyền"));

        try {
            Product p = productRepository.findById(id);
            if (p == null) return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy"));

            p.setActive(!p.isActive());
            productRepository.update(p);
            return ResponseEntity.ok(Map.of("success", true, "active", p.isActive()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════
    // 7. XOÁ MỀM SẢN PHẨM
    // ══════════════════════════════════════════════════════
    @PostMapping("/products/{id}/delete")
    public String deleteProduct(HttpSession session,
                                @PathVariable String id,
                                RedirectAttributes ra) throws Exception {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/login?redirect=/admin";

        try {
            productRepository.deactivate(id);
            ra.addFlashAttribute("success", "✅ Đã ẩn sản phẩm thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Lỗi: " + e.getMessage());
        }
        return "redirect:/admin";
    }
}