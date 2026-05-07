package dinhlam2901.sunilies.controller;

import dinhlam2901.sunilies.model.Product;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ProductController {

    @GetMapping("/products/{handle}")
    public String productDetail(@PathVariable String handle, Model model) {
        // Tìm sản phẩm theo handle — sau thay bằng DB
        Product product = new Product(handle, "Tên sản phẩm", 299000.0, 399000.0, "/images/img_cus_1.jpg");
        model.addAttribute("product", product);
        return "product";
    }

    @GetMapping("/collections")
    public String collections(Model model) {
        return "collection";
    }

    @GetMapping("/collections/{handle}")
    public String collection(@PathVariable String handle, Model model) {
        model.addAttribute("handle", handle);
        return "collection";
    }
}