package group3.recipe.controller;

import group3.recipe.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

  // 코드 검증
  @PostMapping("/verify")
  public ResponseEntity<?> verify(@RequestParam String email, @RequestParam String code) {
    boolean ok = service.verifyCode(email, code);
    return ok ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
  }
}