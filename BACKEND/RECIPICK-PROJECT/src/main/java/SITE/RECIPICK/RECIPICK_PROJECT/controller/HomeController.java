package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/") // 이거만 유지하거나 redirect 용으로 남길 수 있음
    public String root() {
        return "redirect:/home.html";
    }

    @GetMapping("/signup")
    public String signupForm() {
        return "forward:/signup.html"; // forward로 정적 HTML 직접 반환
    }
}