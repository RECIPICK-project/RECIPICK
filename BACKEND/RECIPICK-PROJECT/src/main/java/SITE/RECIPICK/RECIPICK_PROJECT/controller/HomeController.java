package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/") // 이거만 유지하거나 redirect 용으로 남길 수 있음
    public String root() {
        return "redirect:/pages/main.html"; // pages 폴더의 메인 페이지로 변경
    }

    @GetMapping("/test") // 테스트 페이지 추가
    public String test() {
        return "home"; // templates/home.html 렌더링
    }

    @GetMapping("/signup")
    public String signupForm() {
        return "forward:/signup.html"; // forward로 정적 HTML 직접 반환
    }
}
