package dinhlam2901.sunilies.controller;

import dinhlam2901.sunilies.model.Blog;
import dinhlam2901.sunilies.model.HeroSlide;
import dinhlam2901.sunilies.model.Order;
import dinhlam2901.sunilies.model.Product;
import dinhlam2901.sunilies.model.User;
import dinhlam2901.sunilies.repository.BlogRepository;
import dinhlam2901.sunilies.repository.HeroSlideRepository;
import dinhlam2901.sunilies.repository.OrderRepository;
import dinhlam2901.sunilies.repository.ProductRepository;
import dinhlam2901.sunilies.service.AuthService;
import dinhlam2901.sunilies.service.FirebaseStorageService;
import jakarta.servlet.http.HttpSession;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private AuthService             authService;
    @Autowired private ProductRepository       productRepository;
    @Autowired private OrderRepository         orderRepository;
    @Autowired private BlogRepository          blogRepository;
    @Autowired private HeroSlideRepository     heroSlideRepository;
    @Autowired private FirebaseStorageService  storageService;

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

        // Hero slides
        java.util.List<HeroSlide> heroSlides = java.util.List.of();
        try { heroSlides = heroSlideRepository.findAll(); }
        catch (Exception e) { System.err.println("Load hero slides lỗi: " + e.getMessage()); }
        model.addAttribute("heroSlides", heroSlides);

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
                             @RequestParam(required = false) MultipartFile imageFile,
                             @RequestParam(required = false) MultipartFile[] imageFiles,
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
        
        String finalImageUrl = (imageUrl != null) ? imageUrl.trim() : "";
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                finalImageUrl = storageService.uploadProductImage(imageFile);
            } catch (Exception e) {
                ra.addFlashAttribute("error", "❌ Upload ảnh thất bại: " + e.getMessage());
                return "redirect:/admin?tab=products";
            }
        }

        java.util.List<String> finalSubImages = new java.util.ArrayList<>();
        if (images != null && !images.isBlank()) {
            finalSubImages.addAll(split(images));
        }
        if (imageFiles != null && imageFiles.length > 0) {
            for (MultipartFile file : imageFiles) {
                if (file != null && !file.isEmpty()) {
                    try {
                        String url = storageService.uploadProductImage(file);
                        finalSubImages.add(url);
                    } catch (Exception e) {
                        ra.addFlashAttribute("error", "❌ Upload ảnh phụ thất bại: " + e.getMessage());
                        return "redirect:/admin?tab=products";
                    }
                }
            }
        }

        Product p = new Product();
        p.setName(name.trim()); p.setHandle(handle.trim().toLowerCase().replaceAll("\\s+","-"));
        p.setDescription(description); p.setFullDescription(fullDescription);
        p.setPrice(price); p.setComparePrice(comparePrice);
        p.setImageUrl(finalImageUrl); p.setCategory(category.trim());
        p.setCollection(collection != null ? collection.trim() : null);
        p.setStock(stock); p.setOnSale(onSale); p.setNew(isNew); p.setHot(hot); p.setActive(active);
        p.setMetaTitle(metaTitle); p.setMetaDescription(metaDescription);
        p.setImages(finalSubImages);
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
                                @RequestParam(required = false) MultipartFile imageFile,
                                @RequestParam(required = false) MultipartFile[] imageFiles,
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
            
            if (imageFile != null && !imageFile.isEmpty()) {
                p.setImageUrl(storageService.uploadProductImage(imageFile));
            } else if (imageUrl != null && !imageUrl.isBlank()) {
                p.setImageUrl(imageUrl.trim());
            }

            java.util.List<String> finalSubImages = new java.util.ArrayList<>();
            if (images != null && !images.isBlank()) {
                finalSubImages.addAll(split(images));
            }
            if (imageFiles != null && imageFiles.length > 0) {
                for (MultipartFile file : imageFiles) {
                    if (file != null && !file.isEmpty()) {
                        String url = storageService.uploadProductImage(file);
                        finalSubImages.add(url);
                    }
                }
            }
            p.setImages(finalSubImages);

            p.setCategory(category); p.setCollection(collection);
            p.setStock(stock); p.setOnSale(onSale); p.setNew(isNew); p.setHot(hot); p.setActive(active);
            p.setMetaTitle(metaTitle); p.setMetaDescription(metaDescription);
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
                          @RequestParam(required = false) MultipartFile imageFile,
                          @RequestParam(defaultValue = "") String category,
                          @RequestParam(defaultValue = "5") int readTime,
                          @RequestParam(defaultValue = "false") boolean published,
                          @RequestParam(defaultValue = "false") boolean featured,
                          @RequestParam(required = false) String metaTitle,
                          @RequestParam(required = false) String metaDescription,
                          RedirectAttributes ra) throws Exception {
        if (requireAdmin(session) == null) return "redirect:/login?redirect=/admin";
        String imageUrl = "";
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                imageUrl = storageService.uploadBlogImage(imageFile);
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Upload ảnh thất bại: " + e.getMessage());
            return "redirect:/admin?tab=blogs";
        }
        Blog b = new Blog();
        b.setTitle(title.trim());
        b.setSlug(slug.trim().toLowerCase().replaceAll("\\s+", "-"));
        b.setExcerpt(excerpt);
        b.setContent(sanitizeBlogContent(content));   // Sanitize HTML — chống stored XSS
        b.setImageUrl(imageUrl);
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
                             @RequestParam(required = false) MultipartFile imageFile,
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
            b.setContent(sanitizeBlogContent(content));   // Sanitize HTML — chống stored XSS
            if (imageFile != null && !imageFile.isEmpty()) {
                b.setImageUrl(storageService.uploadBlogImage(imageFile));
            } else if (imageUrl != null && !imageUrl.isBlank()) {
                b.setImageUrl(imageUrl.trim());
            }
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

    // ══════════════════════════════════════════════════════
    // HERO SLIDES – THÊM
    // ══════════════════════════════════════════════════════
    @PostMapping("/hero/add")
    public String addHeroSlide(HttpSession session,
                               @RequestParam(required = false) MultipartFile imageFile,
                               @RequestParam(defaultValue = "") String subtitle,
                               @RequestParam(defaultValue = "") String titleLine1,
                               @RequestParam(defaultValue = "") String titleLine2,
                               @RequestParam(defaultValue = "Khám phá ngay") String buttonText,
                               @RequestParam(defaultValue = "/collections") String buttonLink,
                               @RequestParam(defaultValue = "0") int sortOrder,
                               @RequestParam(defaultValue = "true") boolean active,
                               RedirectAttributes ra) throws Exception {
        if (requireAdmin(session) == null) return "redirect:/login?redirect=/admin";
        String imageUrl = "";
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                imageUrl = storageService.uploadHeroImage(imageFile);
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Upload ảnh thất bại: " + e.getMessage());
            return "redirect:/admin?tab=hero";
        }
        HeroSlide s = new HeroSlide();
        s.setImageUrl(imageUrl);
        s.setSubtitle(subtitle.trim());
        s.setTitleLine1(titleLine1.trim());
        s.setTitleLine2(titleLine2.trim());
        s.setButtonText(buttonText.trim());
        s.setButtonLink(buttonLink.trim());
        s.setSortOrder(sortOrder);
        s.setActive(active);
        try { heroSlideRepository.save(s); ra.addFlashAttribute("success", "✅ Thêm slide thành công!"); }
        catch (Exception e) { ra.addFlashAttribute("error", "❌ " + e.getMessage()); }
        return "redirect:/admin?tab=hero";
    }

    // ── CẬP NHẬT HERO SLIDE ──────────────────────────────
    @PostMapping("/hero/{id}/update")
    public String updateHeroSlide(HttpSession session, @PathVariable String id,
                                  @RequestParam(required = false) MultipartFile imageFile,
                                  @RequestParam(required = false) String imageUrl,
                                  @RequestParam(defaultValue = "") String subtitle,
                                  @RequestParam(defaultValue = "") String titleLine1,
                                  @RequestParam(defaultValue = "") String titleLine2,
                                  @RequestParam(defaultValue = "Khám phá ngay") String buttonText,
                                  @RequestParam(defaultValue = "/collections") String buttonLink,
                                  @RequestParam(defaultValue = "0") int sortOrder,
                                  @RequestParam(defaultValue = "false") boolean active,
                                  RedirectAttributes ra) throws Exception {
        if (requireAdmin(session) == null) return "redirect:/login?redirect=/admin";
        try {
            HeroSlide s = heroSlideRepository.findById(id);
            if (s == null) { ra.addFlashAttribute("error", "Không tìm thấy slide!"); return "redirect:/admin?tab=hero"; }
            if (imageFile != null && !imageFile.isEmpty()) {
                s.setImageUrl(storageService.uploadHeroImage(imageFile));
            } else if (imageUrl != null && !imageUrl.isBlank()) {
                s.setImageUrl(imageUrl.trim());
            }
            s.setSubtitle(subtitle.trim());
            s.setTitleLine1(titleLine1.trim());
            s.setTitleLine2(titleLine2.trim());
            s.setButtonText(buttonText.trim());
            s.setButtonLink(buttonLink.trim());
            s.setSortOrder(sortOrder);
            s.setActive(active);
            heroSlideRepository.update(s);
            ra.addFlashAttribute("success", "✅ Cập nhật slide thành công!");
        } catch (Exception e) { ra.addFlashAttribute("error", "❌ " + e.getMessage()); }
        return "redirect:/admin?tab=hero";
    }

    // ── AJAX: toggle active ───────────────────────────────
    @PostMapping("/hero/{id}/toggle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleHeroSlide(HttpSession session,
                                                               @PathVariable String id,
                                                               @RequestBody Map<String, Boolean> body) throws Exception {
        if (requireAdmin(session) == null) return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        try {
            boolean active = body.getOrDefault("active", false);
            heroSlideRepository.toggleActive(id, active);
            return ResponseEntity.ok(Map.of("success", true, "active", active));
        } catch (Exception e) { return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage())); }
    }

    // ── XOÁ HERO SLIDE ───────────────────────────────────
    @PostMapping("/hero/{id}/delete")
    public String deleteHeroSlide(HttpSession session, @PathVariable String id, RedirectAttributes ra) throws Exception {
        if (requireAdmin(session) == null) return "redirect:/login?redirect=/admin";
        try { heroSlideRepository.hardDelete(id); ra.addFlashAttribute("success", "✅ Đã xoá slide!"); }
        catch (Exception e) { ra.addFlashAttribute("error", "❌ " + e.getMessage()); }
        return "redirect:/admin?tab=hero";
    }

    private List<String> split(String s) {
        return Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isEmpty()).collect(Collectors.toList());
    }

    /**
     * Sanitize HTML blog content với Jsoup Safelist.relaxed().
     * Cho phép: h1-h6, p, div, span, a, img, ul/ol/li, blockquote, pre, code,
     *           b, i, strong, em, br, table... Loại bỏ: script, style, onclick, v.v.
     */
    private String sanitizeBlogContent(String html) {
        if (html == null || html.isBlank()) return html;
        // relaxed() cho phép hầu hết thẻ HTML nội dung nhưng loại bỏ script/event handlers
        Safelist safelist = Safelist.relaxed()
                .addTags("span", "div", "section", "article", "header", "footer", "figure", "figcaption")
                .addAttributes(":all", "class", "id", "style")
                .addAttributes("a", "target", "rel")
                .addAttributes("img", "width", "height", "loading");
        return Jsoup.clean(html, safelist);
    }
}