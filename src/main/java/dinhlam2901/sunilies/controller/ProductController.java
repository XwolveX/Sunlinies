package dinhlam2901.sunilies.controller;

import dinhlam2901.sunilies.model.Product;
import dinhlam2901.sunilies.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    // ─── Trang chi tiết sản phẩm: /products/sandy-bag ────────────
    @GetMapping("/products/{handle}")
    public String productDetail(@PathVariable String handle, Model model) {
        try {
            Product product = productRepository.findByHandle(handle);
            if (product == null) return "redirect:/collections";

            model.addAttribute("product", product);

            // Gợi ý sản phẩm cùng category
            List<Product> related = productRepository.findByCategory(product.getCategory());
            related.removeIf(p -> p.getId().equals(product.getId())); // bỏ sp hiện tại
            model.addAttribute("relatedProducts", related.stream().limit(4).toList());

        } catch (Exception e) {
            System.err.println("❌ Lỗi load product: " + e.getMessage());
            return "redirect:/collections";
        }
        return "product";
    }

    // ─── Trang danh sách tất cả sản phẩm: /collections ───────────
    @GetMapping("/collections")
    public String collections(Model model) {
        try {
            model.addAttribute("products", productRepository.findAll());
        } catch (Exception e) {
            System.err.println("❌ Lỗi load collections: " + e.getMessage());
            model.addAttribute("products", List.of());
        }
        return "collection";
    }

    // ─── Trang collection cụ thể: /collections/dang-ha ───────────
    @GetMapping("/collections/{handle}")
    public String collection(@PathVariable String handle, Model model) {
        try {
            List<Product> products = productRepository.findByCollection(handle);
            model.addAttribute("products", products);
            model.addAttribute("handle", handle);
        } catch (Exception e) {
            System.err.println("❌ Lỗi load collection: " + e.getMessage());
            model.addAttribute("products", List.of());
        }
        return "collection";
    }
}