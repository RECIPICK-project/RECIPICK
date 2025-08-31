package group3.recipe.controller;

import group3.recipe.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/email")
@RequiredArgsConstructor
public class EmailVerificationController {

  private final EmailService emailService;

  // 이메일 인증 코드 발송
  @PostMapping("/send")
  public ResponseEntity<String> sendCode(@RequestParam String email) {
    emailService.sendVerificationCode(email);
    return ResponseEntity.ok("인증 코드가 발송되었습니다.");
  }

  // 인증 코드 검증
  @PostMapping("/verify")
  public ResponseEntity<String> verifyCode(@RequestParam String email,
      @RequestParam String code) {
    boolean success = emailService.verifyCode(email, code);
    return success ? ResponseEntity.ok("인증 성공") : ResponseEntity.badRequest().body("인증 실패");
  }
}