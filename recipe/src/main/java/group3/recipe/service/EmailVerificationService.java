package group3.recipe.service;

import group3.recipe.entity.EmailVerification;
import group3.recipe.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

  private final EmailVerificationRepository repository;
  private final JavaMailSender mailSender;

  // 인증 코드 발송
  public void sendVerificationCode(String email) {
    String code = generateCode();
    EmailVerification entity = EmailVerification.builder()
        .email(email)
        .code(code)
        .used(false)
        .build();
    repository.save(entity);

    // 이메일 발송
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(email);
    message.setSubject("[Recipe] 이메일 인증 코드");
    message.setText("인증 코드: " + code);
    mailSender.send(message);
  }

  // 코드 검증
  public boolean verifyCode(String email, String code) {
    var optional = repository.findByEmailAndCodeAndUsedFalse(email, code);
    if (optional.isPresent()) {
      EmailVerification entity = optional.get();
      entity.setUsed(true);
      repository.save(entity);
      return true;
    }
    return false;
  }

  // 이메일 인증 여부 확인
  public boolean isEmailVerified(String email) {
    return repository.existsByEmailAndUsedTrue(email);
  }

  private String generateCode() {
    Random random = new Random();
    int num = 100000 + random.nextInt(900000); // 6자리
    return String.valueOf(num);
  }
}