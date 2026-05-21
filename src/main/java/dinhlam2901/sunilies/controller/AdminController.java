package dinhlam2901.sunilies.controller;

import dinhlam2901.sunilies.model.Blog;
import dinhlam2901.sunilies.model.Order;
import dinhlam2901.sunilies.model.Product;
import dinhlam2901.sunilies.model.User;
import dinhlam2901.sunilies.repository.BlogRepository;
import dinhlam2901.sunilies.repository.OrderRepository;
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

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private AuthService       authService;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository   orderRepository;
    @Autowired private BlogRepository    blogRepository;

    private User requireAdmin(HttpSession session) throws Exception {
        if (!authService.isLoggedIn(session)) return null;
        User user = authService.getCurrentUser(session);
        if (user == null || !"ADMIN".equals(user.getRole())) return null;
        return user;
    }

    // ══════════════════════════════════════════════════════
    // DASHBOARD – load tất cả dữ liệu
    // ══════════════════════════════════════════════════════
    @GetMapping({"", "/"})
    public String dashboard(HttpSession session, Model model,
                            @RequestParam(defaultValue = "stats") String tab) throws Exception {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/login?redirect=/admin";

        List<Product> products = productRepository.findAllAdmin();
        long totalProducts  = products.size();
        long activeProducts = products.stream().filter(Product::isActive).count();
        long outOfStock     = products.stream().filter(p -> p.getStock() <= 0).count();
        long lowStock       = products.stream().filter(p -> p.getStock() > 0 && p.getStock() <= 5).count();
        double totalValue   = products.stream()
                .mapToDouble(p -> (p.getPrice() != null ? p.getPrice() : 0) * p.getStock()).sum();

        List<Order> orders = List.of();
        try { orders = orderRepository.findAllOrders(); }
        catch (Exception e) { System.err.println("Load orders lỗi: " + e.getMessage()); }

        long   totalOrders   = orders.size();
        long   pendingOrders = orders.stream().filter(o -> "PENDING".equals(o.getStatus())).count();
        long   paidOrders    = orders.stream().filter(o -> "PAID".equals(o.getStatus())).count();
        double totalRevenue  = orders.stream().filter(o -> "PAID".equals(o.getStatus()))
                .mapToDouble(Order::getTotalAmount).sum();

        // Blog stats
        java.util.List<Blog> blogs = java.util.List.of();
        try { blogs = blogRepository.findAllAdmin(); }
        catch (Exception e) { System.err.println("Load blogs lỗi: " + e.getMessage()); }
        long totalBlogs     = blogs.size();
        long publishedBlogs = blogs.stream().filter(Blog::isPublished).count();

        model.addAttribute("admin",          admin);
        model.addAttribute("products",       products);
        model.addAttribute("orders",         orders);
        model.addAttribute("blogs",          blogs);
        model.addAttribute("activeTab",      tab);
        model.addAttribute("totalProducts",  totalProducts);
        model.addAttribute("activeProducts", activeProducts);
        model.addAttribute("outOfStock",     outOfStock);
        model.addAttribute("lowStock",       lowStock);
        model.addAttribute("totalValue",     totalValue);
        model.addAttribute("totalOrders",    totalOrders);
        model.addAttribute("pendingOrders",  pendingOrders);
        model.addAttribute("paidOrders",     paidOrders);
        model.addAttribute("totalRevenue",   totalRevenue);
        model.addAttribute("totalBlogs",     totalBlogs);
        model.addAttribute("publishedBlogs", publishedBlogs);
        return "admin/dashboard";
    }

    // ══════════════════════════════════════════════════════
    // THÊM SẢN PHẨM
    // ══════════════════════════════════════════════════════
    @PostMapping("/products/add")
    public String addProduct(HttpSession session,
                             @RequestParam String name, @RequestParam String handle,
                             @RequestParam(defaultValue = "") String description,
                             @RequestParam(required = false) String fullDescription,
                             @RequestParam Double price, @RequestParam(required = false) Double comparePrice,
                             @RequestParam(defaultValue = "") String imageUrl,
                             @RequestParam(required = false) String images,
                             @RequestParam(defaultValue = "") String category,
                             @RequestParam(required = false) String collection,
                             @RequestParam(required = false) String sizes, @RequestParam(required = false) String colors,
                             @RequestParam(required = false) String tags,
                             @RequestParam(defaultValue = "0") int stock,
                             @RequestParam(defaultValue = "false") boolean onSale,
                             @RequestParam(defaultValue = "false") boolean isNew,
                             @RequestParam(defaultValue = "false") boolean hot,
                             @RequestParam(defaultValue = "false") boolean active,
                             @RequestParam(required = false) String metaTitle,
                             @RequestParam(required = false) String metaDescription,
                             RedirectAttributes ra) throws Exception {

        if (requireAdmin(session) == null) return "redirect:/login?redirect=/admin";
        Product p = new Product();
        p.setName(name.trim()); p.setHandle(handle.trim().toLowerCase().replaceAll("\\s+","-"));
        p.setDescription(description); p.setFullDescription(fullDescription);
        p.setPrice(price); p.setComparePrice(comparePrice);
        p.setImageUrl(imageUrl.trim()); p.setCategory(category.trim());
        p.setCollection(collection != null ? collection.trim() : null);
        p.setStock(stock); p.setOnSale(onSale); p.setNew(isNew); p.setHot(hot); p.setActive(active);
        p.setMetaTitle(metaTitle); p.setMetaDescription(metaDescription);
        if (images != null && !images.isBlank()) p.setImages(split(images));
        if (sizes  != null && !sizes.isBlank())  p.setSizes(split(sizes));
        if (colors != null && !colors.isBlank()) p.setColors(split(colors));
        if (tags   != null && !tags.isBlank())   p.setTags(split(tags));
        try { productRepository.save(p); ra.addFlashAttribute("success","✅ Thêm \""+name+"\" thành công!"); }
        catch (Exception e) { ra.addFlashAttribute("error","❌ "+e.getMessage()); }
        return "redirect:/admin?tab=products";
    }

    // ══════════════════════════════════════════════════════
    // CẬP NHẬT SẢN PHẨM
    // ══════════════════════════════════════════════════════
    @PostMapping("/products/{id}/update")
    public String updateProduct(HttpSession session, @PathVariable String id,
                                @RequestParam String name, @RequestParam String handle,
                                @RequestParam(defaultValue = "") String description,
                                @RequestParam(required = false) String fullDescription,
                                @RequestParam Double price, @RequestParam(required = false) Double comparePrice,
                                @RequestParam(required = false) String imageUrl,
                                @RequestParam(required = false) String images,
                                @RequestParam(required = false) String category,
                                @RequestParam(required = false) String collection,
                                @RequestParam(required = false) String sizes, @RequestParam(required = false) String colors,
                                @RequestParam(required = false) String tags,
                                @RequestParam(defaultValue = "0") int stock,
                                @RequestParam(defaultValue = "false") boolean onSale,
                                @RequestParam(defaultValue = "false") boolean isNew,
                                @RequestParam(defaultValue = "false") boolean hot,
                                @RequestParam(defaultValue = "false") boolean active,
                                @RequestParam(required = false) String metaTitle,
                                @RequestParam(required = false) String metaDescription,
                                RedirectAttributes ra) throws Exception {

        if (requireAdmin(session) == null) return "redirect:/login?redirect=/admin";
        try {
            Product p = productRepository.findById(id);
            if (p == null) { ra.addFlashAttribute("error","Không tìm thấy!"); return "redirect:/admin?tab=products"; }
            p.setName(name.trim()); p.setHandle(handle.trim().toLowerCase().replaceAll("\\s+","-"));
            p.setDescription(description); p.setFullDescription(fullDescription);
            p.setPrice(price); p.setComparePrice(comparePrice);
            if (imageUrl != null && !imageUrl.isBlank()) p.setImageUrl(imageUrl.trim());
            p.setCategory(category); p.setCollection(collection);
            p.setStock(stock); p.setOnSale(onSale); p.setNew(isNew); p.setHot(hot); p.setActive(active);
            p.setMetaTitle(metaTitle); p.setMetaDescription(metaDescription);
            if (images != null && !images.isBlank()) p.setImages(split(images));
            if (sizes  != null && !sizes.isBlank())  p.setSizes(split(sizes));
            if (colors != null && !colors.isBlank()) p.setColors(split(colors));
            if (tags   != null && !tags.isBlank())   p.setTags(split(tags));
            productRepository.update(p);
            ra.addFlashAttribute("success","✅ Cập nhật \""+name+"\" thành công!");
        } catch (Exception e) { ra.addFlashAttribute("error","❌ "+e.getMessage()); }
        return "redirect:/admin?tab=products";
    }

    // ── AJAX: cập nhật giá ────────────────────────────────
    @PostMapping("/products/{id}/price")
    @ResponseBody
    public ResponseEntity<Map<String,Object>> updatePrice(HttpSession session,
                                                          @PathVariable String id, @RequestBody Map<String,Double> body) throws Exception {
        if (requireAdmin(session) == null) return ResponseEntity.status(403).body(Map.of("error","Forbidden"));
        Product p = productRepository.findById(id);
        if (p == null) return ResponseEntity.badRequest().body(Map.of("error","Không tồn tại"));
        if (body.containsKey("price")) p.setPrice(body.get("price"));
        if (body.containsKey("comparePrice")) p.setComparePrice(body.get("comparePrice"));
        productRepository.update(p);
        return ResponseEntity.ok(Map.of("success",true,"price",p.getPrice()));
    }

    // ── AJAX: cập nhật tồn kho ────────────────────────────
    @PostMapping("/products/{id}/stock")
    @ResponseBody
    public ResponseEntity<Map<String,Object>> updateStock(HttpSession session,
                                                          @PathVariable String id, @RequestBody Map<String,Integer> body) throws Exception {
        if (requireAdmin(session) == null) return ResponseEntity.status(403).body(Map.of("error","Forbidden"));
        Product p = productRepository.findById(id);
        if (p == null) return ResponseEntity.badRequest().body(Map.of("error","Không tồn tại"));
        p.setStock(body.getOrDefault("stock", p.getStock()));
        productRepository.update(p);
        return ResponseEntity.ok(Map.of("success",true,"stock",p.getStock()));
    }

    // ── AJAX: toggle active ───────────────────────────────
    @PostMapping("/products/{id}/toggle")
    @ResponseBody
    public ResponseEntity<Map<String,Object>> toggleActive(HttpSession session, @PathVariable String id) throws Exception {
        if (requireAdmin(session) == null) return ResponseEntity.status(403).body(Map.of("error","Forbidden"));
        Product p = productRepository.findById(id);
        if (p == null) return ResponseEntity.badRequest().body(Map.of("error","Không tìm thấy"));
        p.setActive(!p.isActive());
        productRepository.update(p);
        return ResponseEntity.ok(Map.of("success",true,"active",p.isActive()));
    }

    // ── Ẩn sản phẩm (soft delete) ────────────────────────
    @PostMapping("/products/{id}/hide")
    public String hideProduct(HttpSession session, @PathVariable String id, RedirectAttributes ra) throws Exception {
        if (requireAdmin(session) == null) return "redirect:/login?redirect=/admin";
        try { productRepository.deactivate(id); ra.addFlashAttribute("success","✅ Đã ẩn sản phẩm!"); }
        catch (Exception e) { ra.addFlashAttribute("error","❌ "+e.getMessage()); }
        return "redirect:/admin?tab=products";
    }

    // ── Xoá vĩnh viễn (hard delete) ──────────────────────
    @PostMapping("/products/{id}/delete")
    public String deleteProduct(HttpSession session, @PathVariable String id, RedirectAttributes ra) throws Exception {
        if (requireAdmin(session) == null) return "redirect:/login?redirect=/admin";
        try { productRepository.hardDelete(id); ra.addFlashAttribute("success","✅ Đã xoá vĩnh viễn sản phẩm!"); }
        catch (Exception e) { ra.addFlashAttribute("error","❌ "+e.getMessage()); }
        return "redirect:/admin?tab=products";
    }

    // ── AJAX: cập nhật trạng thái đơn hàng ───────────────
    @PostMapping("/orders/{id}/status")
    @ResponseBody
    public ResponseEntity<Map<String,Object>> updateOrderStatus(HttpSession session,
                                                                @PathVariable String id, @RequestBody Map<String,String> body) throws Exception {
        if (requireAdmin(session) == null) return ResponseEntity.status(403).body(Map.of("error","Forbidden"));
        String newStatus = body.get("status");
        if (newStatus == null) return ResponseEntity.badRequest().body(Map.of("error","Thiếu status"));
        orderRepository.updateStatus(id, newStatus);
        return ResponseEntity.ok(Map.of("success",true,"status",newStatus));
    }

    // ══════════════════════════════════════════════════════
    // THÊM BÀI VIẾT BLOG
    // ══════════════════════════════════════════════════════
    @PostMapping("/blogs/add")
    public String addBlog(HttpSession session,
                          @RequestParam String title,
                          @RequestParam String slug,
                          @RequestParam(defaultValue = "") String excerpt,
                          @RequestParam(required = false) String content,
                          @RequestParam(defaultValue = "") String imageUrl,
                          @RequestParam(defaultValue = "") String category,
                          @RequestParam(defaultValue = "5") int readTime,
                          @RequestParam(defaultValue = "false") boolean published,
                          @RequestParam(defaultValue = "false") boolean featured,
                          @RequestParam(required = false) String metaTitle,
                          @RequestParam(required = false) String metaDescription,
                          RedirectAttributes ra) throws Exception {
        if (requireAdmin(session) == null) return "redirect:/login?redirect=/admin";
        Blog b = new Blog();
        b.setTitle(title.trim());
        b.setSlug(slug.trim().toLowerCase().replaceAll("\\s+", "-"));
        b.setExcerpt(excerpt);
        b.setContent(content);
        b.setImageUrl(imageUrl.trim());
        b.setCategory(category.trim());
        b.setReadTime(readTime);
        b.setPublished(published);
        b.setFeatured(featured);
        b.setMetaTitle(metaTitle);
        b.setMetaDescription(metaDescription);
        b.setCreatedAt(System.currentTimeMillis());
        b.setUpdatedAt(System.currentTimeMillis());
        try {
            blogRepository.save(b);
            ra.addFlashAttribute("success", "✅ Thêm bài viết \"" + title + "\" thành công!");
        } catch (Exception e) { ra.addFlashAttribute("error", "❌ " + e.getMessage()); }
        return "redirect:/admin?tab=blogs";
    }

    // ══════════════════════════════════════════════════════
    // CẬP NHẬT BÀI VIẾT BLOG
    // ══════════════════════════════════════════════════════
    @PostMapping("/blogs/{id}/update")
    public String updateBlog(HttpSession session, @PathVariable String id,
                             @RequestParam String title,
                             @RequestParam String slug,
                             @RequestParam(defaultValue = "") String excerpt,
                             @RequestParam(required = false) String content,
                             @RequestParam(required = false) String imageUrl,
                             @RequestParam(defaultValue = "") String category,
                             @RequestParam(defaultValue = "5") int readTime,
                             @RequestParam(defaultValue = "false") boolean published,
                             @RequestParam(defaultValue = "false") boolean featured,
                             @RequestParam(required = false) String metaTitle,
                             @RequestParam(required = false) String metaDescription,
                             RedirectAttributes ra) throws Exception {
        if (requireAdmin(session) == null) return "redirect:/login?redirect=/admin";
        try {
            Blog b = blogRepository.findById(id);
            if (b == null) { ra.addFlashAttribute("error", "Không tìm thấy bài viết!"); return "redirect:/admin?tab=blogs"; }
            b.setTitle(title.trim());
            b.setSlug(slug.trim().toLowerCase().replaceAll("\\s+", "-"));
            b.setExcerpt(excerpt);
            b.setContent(content);
            if (imageUrl != null && !imageUrl.isBlank()) b.setImageUrl(imageUrl.trim());
            b.setCategory(category.trim());
            b.setReadTime(readTime);
            b.setPublished(published);
            b.setFeatured(featured);
            b.setMetaTitle(metaTitle);
            b.setMetaDescription(metaDescription);
            b.setUpdatedAt(System.currentTimeMillis());
            blogRepository.update(b);
            ra.addFlashAttribute("success", "✅ Cập nhật \"" + title + "\" thành công!");
        } catch (Exception e) { ra.addFlashAttribute("error", "❌ " + e.getMessage()); }
        return "redirect:/admin?tab=blogs";
    }

    // ── AJAX: toggle published ────────────────────────────
    @PostMapping("/blogs/{id}/toggle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleBlogPublished(HttpSession session, @PathVariable String id) throws Exception {
        if (requireAdmin(session) == null) return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        try {
            boolean published = blogRepository.togglePublished(id);
            return ResponseEntity.ok(Map.of("success", true, "published", published));
        } catch (Exception e) { return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage())); }
    }

    // ── Xoá vĩnh viễn bài viết ───────────────────────────
    @PostMapping("/blogs/{id}/delete")
    public String deleteBlog(HttpSession session, @PathVariable String id, RedirectAttributes ra) throws Exception {
        if (requireAdmin(session) == null) return "redirect:/login?redirect=/admin";
        try { blogRepository.hardDelete(id); ra.addFlashAttribute("success", "✅ Đã xoá bài viết!"); }
        catch (Exception e) { ra.addFlashAttribute("error", "❌ " + e.getMessage()); }
        return "redirect:/admin?tab=blogs";
    }

    private List<String> split(String s) {
        return Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isEmpty()).collect(Collectors.toList());
    }
}