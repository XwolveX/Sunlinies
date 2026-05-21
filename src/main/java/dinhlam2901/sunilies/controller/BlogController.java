package dinhlam2901.sunilies.controller;

import dinhlam2901.sunilies.model.Blog;
import dinhlam2901.sunilies.repository.BlogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class BlogController {

    @Autowired
    private BlogRepository blogRepository;

    // ── Danh sách bài viết (/blogs) ───────────────────────────────
    @GetMapping("/blogs")
    public String blogListPage(Model model) {
        try {
            List<Blog> blogs    = blogRepository.findAllPublished();
            Blog       featured = blogRepository.findFeatured();

            // Nếu không có bài featured riêng, lấy bài đầu tiên
            if (featured == null && !blogs.isEmpty()) {
                featured = blogs.get(0);
            }

            model.addAttribute("blogs",    blogs);
            model.addAttribute("featured", featured);
        } catch (Exception e) {
            System.err.println("Lỗi load blogs: " + e.getMessage());
            model.addAttribute("blogs",    List.of());
            model.addAttribute("featured", null);
        }
        return "blog";
    }

    // ── Chi tiết bài viết (/blogs/{slug}) ────────────────────────
    @GetMapping("/blogs/{slug}")
    public String blogDetailPage(@PathVariable String slug, Model model) {
        try {
            Blog blog = blogRepository.findBySlug(slug);
            if (blog == null || !blog.isPublished()) return "redirect:/blogs";

            // Gợi ý bài khác cùng category
            List<Blog> related = blogRepository.findAllPublished().stream()
                    .filter(b -> !b.getId().equals(blog.getId()))
                    .filter(b -> blog.getCategory() != null && blog.getCategory().equals(b.getCategory()))
                    .limit(3)
                    .toList();

            model.addAttribute("blog",    blog);
            model.addAttribute("related", related);
        } catch (Exception e) {
            System.err.println("Lỗi load blog detail: " + e.getMessage());
            return "redirect:/blogs";
        }
        return "blog-detail";
    }
}
