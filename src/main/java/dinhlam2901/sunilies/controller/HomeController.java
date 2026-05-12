package dinhlam2901.sunilies.controller;

import dinhlam2901.sunilies.model.Category;
import dinhlam2901.sunilies.model.Product;
import dinhlam2901.sunilies.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private ProductRepository productRepository;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("categories", getCategories());

        try {
            // Sản phẩm bán chạy (hot = true)
            List<Product> hotProducts = productRepository.findHotProducts(8);
            model.addAttribute("hotProducts", hotProducts);

            // Sản phẩm mới (isNew = true)
            List<Product> newProducts = productRepository.findNewProducts(8);
            model.addAttribute("newProducts", newProducts);

        } catch (Exception e) {
            System.err.println("❌ Lỗi load trang chủ: " + e.getMessage());
            model.addAttribute("hotProducts", List.of());
            model.addAttribute("newProducts", List.of());
        }

        return "index";
    }

    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("query", q);
        try {
            // Tìm kiếm đơn giản: lấy tất cả rồi filter theo tên
            if (q != null && !q.isBlank()) {
                String keyword = q.toLowerCase();
                List<Product> results = productRepository.findAll().stream()
                        .filter(p -> p.getName() != null &&
                                p.getName().toLowerCase().contains(keyword))
                        .toList();
                model.addAttribute("products", results);
            } else {
                model.addAttribute("products", List.of());
            }
        } catch (Exception e) {
            model.addAttribute("products", List.of());
        }
        return "search";
    }

    @GetMapping("/newsletter")
    public String newsletter(@RequestParam String email) {
        // TODO: lưu email vào Firestore collection "subscribers"
        System.out.println("📧 Email đăng ký: " + email);
        return "redirect:/";
    }

    // ─── Categories tĩnh (có thể chuyển sang Firestore sau) ──────
    private List<Category> getCategories() {
        return List.of(
                new Category("ao", "Áo", "/image/collection_banner.jpg"),
                new Category("quan", "Quần", "/image/collection_banner_0.jpg"),
                new Category("vay", "Váy", "/image/image-parallax-1.jpg"),
                new Category("phu-kien", "Phụ kiện", "/image/image-parallax-2.jpg")
        );
    }
}