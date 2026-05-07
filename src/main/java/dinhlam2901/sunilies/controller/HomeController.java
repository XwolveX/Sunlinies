package dinhlam2901.sunilies.controller;

import dinhlam2901.sunilies.model.Category;
import dinhlam2901.sunilies.model.Product;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Model model) {
        // Dữ liệu mẫu — sau này thay bằng database
        model.addAttribute("categories", getSampleCategories());
        model.addAttribute("featuredProducts", getSampleProducts());
        return "index";
    }

    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }

    @GetMapping("/search")
    public String search(String q, Model model) {
        model.addAttribute("query", q);
        model.addAttribute("products", List.of()); // Sau thay bằng search thật
        return "search";
    }

    @GetMapping("/newsletter")
    public String newsletter(String email) {
        // Xử lý đăng ký email
        return "redirect:/";
    }

    private List<Category> getSampleCategories() {
        return List.of(
                new Category("ao", "Áo", "/image/collection_banner.jpg"),
                new Category("quan", "Quần", "/image/collection_banner_0.jpg"),
                new Category("vay", "Váy", "/image/image-parallax-1.jpg"),
                new Category("phu-kien", "Phụ kiện", "/image/image-parallax-2.jpg")
        );
    }

    private List<Product> getSampleProducts() {
        return List.of(
                new Product("sp-1", "Áo thun basic", 299000.0, 399000.0, "/image/img_cus_1.jpg"),
                new Product("sp-2", "Quần jeans slim", 499000.0, null, "/image/img_cus_2.jpg"),
                new Product("sp-3", "Váy hoa nhí", 399000.0, 550000.0, "/image/img_cus_3.jpg"),
                new Product("sp-4", "Áo khoác denim", 699000.0, null, "/image/img_cus_4.jpg")
        );
    }
}