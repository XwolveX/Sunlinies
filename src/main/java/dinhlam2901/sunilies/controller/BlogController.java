package dinhlam2901.sunilies.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BlogController {

    @GetMapping("/blogs")
    public String blogPage() {
        return "blog";
    }
}
