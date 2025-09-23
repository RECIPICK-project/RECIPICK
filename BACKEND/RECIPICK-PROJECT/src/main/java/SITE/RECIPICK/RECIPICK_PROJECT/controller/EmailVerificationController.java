package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import SITE.RECIPICK.RECIPICK_PROJECT.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/email")
public class EmailVerificationController {

    private final EmailVerificationService service;

    // 인증코드 발송
    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestParam String email) {
        service.sendVerificationCode(email);
        return ResponseEntity.ok().build();
    }

    // ✅ 재전송 추가
    @PostMapping("/resend")
    public ResponseEntity<?> resend(@RequestParam String email) {
        service.sendVerificationCode(email); // 내부에서 쿨다운 처리(409/429)
        return ResponseEntity.ok().build();
    }

    // 코드 검증
    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam String email, @RequestParam String code) {
        boolean ok = service.verifyCode(email, code);
        return ok ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }
}
