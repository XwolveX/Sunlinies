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
import java.util.stream.Collectors;

@Controller
public class HomeController {

    @Autowired
    private ProductRepository productRepository;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("categories", getCategories());

        try {
            List<Product> hotProducts = productRepository.findHotProducts(8);
            model.addAttribute("hotProducts", hotProducts);

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

    // ══════════════════════════════════════════════════════
    // SEARCH — tìm kiếm sản phẩm theo keyword
    // Tìm trong: name, description, category, tags, collection
    // ══════════════════════════════════════════════════════
    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("query", q);

        // Không có keyword → trả về trang trống (gợi ý tìm kiếm)
        if (q == null || q.isBlank()) {
            model.addAttribute("products", List.of());
            return "search";
        }

        try {
            String keyword = q.trim().toLowerCase();

            List<Product> results = productRepository.findAll().stream()
                    .filter(p -> matchesKeyword(p, keyword))
                    // Ưu tiên: tên chứa keyword lên đầu, sau đó description/tags
                    .sorted((a, b) -> {
                        boolean aName = a.getName() != null && a.getName().toLowerCase().contains(keyword);
                        boolean bName = b.getName() != null && b.getName().toLowerCase().contains(keyword);
                        if (aName && !bName) return -1;
                        if (!aName && bName) return 1;
                        return 0;
                    })
                    .collect(Collectors.toList());

            model.addAttribute("products", results);

            System.out.println("🔍 Tìm kiếm '" + q + "' → " + results.size() + " kết quả");

        } catch (Exception e) {
            System.err.println("❌ Lỗi tìm kiếm: " + e.getMessage());
            model.addAttribute("products", List.of());
        }

        return "search";
    }
    /**
     * Kiểm tra xem product có khớp với keyword không.
     * Tìm trong: name, description, category, tags, collection
     */
    private boolean matchesKeyword(Product p, String keyword) {
        // Tìm trong tên (quan trọng nhất)
        if (p.getName() != null && p.getName().toLowerCase().contains(keyword)) return true;

        // Tìm trong mô tả ngắn
        if (p.getDescription() != null && p.getDescription().toLowerCase().contains(keyword)) return true;

        // Tìm trong category
        if (p.getCategory() != null && p.getCategory().toLowerCase().contains(keyword)) return true;

        // Tìm trong collection
        if (p.getCollection() != null && p.getCollection().toLowerCase().contains(keyword)) return true;

        // Tìm trong tags
        if (p.getTags() != null) {
            for (String tag : p.getTags()) {
                if (tag != null && tag.toLowerCase().contains(keyword)) return true;
            }
        }

        return false;
    }

    @GetMapping("/newsletter")
    public String newsletter(@RequestParam String email) {
        System.out.println("📧 Email đăng ký: " + email);
        return "redirect:/";
    }

    // ─── Categories tĩnh ──────────────────────────────────────────
    private List<Category> getCategories() {
        return List.of(
                new Category("ao", "Áo", "/image/collection_banner.jpg"),
                new Category("quan", "Quần", "/image/collection_banner_0.jpg"),
                new Category("vay", "Váy", "/image/image-parallax-1.jpg"),
                new Category("phu-kien", "Phụ kiện", "/image/image-parallax-2.jpg")
        );
    }
}