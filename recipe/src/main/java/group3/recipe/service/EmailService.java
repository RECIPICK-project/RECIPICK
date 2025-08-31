package group3.recipe.service;

import group3.recipe.entity.EmailVerification;
import group3.recipe.repository.EmailVerificationRepository;
import group3.recipe.repository.UserRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

  private final EmailVerificationRepository verificationRepository;
  private final JavaMailSender mailSender;
  private final UserRepository userRepository;

  // 코드 발송
  public void sendVerificationCode(String email) {
    String code = generateCode();
    LocalDateTime expireAt = LocalDateTime.now().plusMinutes(10);

    verificationRepository.save(
        EmailVerification.builder()
            .email(email)
            .code(code)
            .expireAt(expireAt)
            .used(false)
            .build()
    );

    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(email);
    message.setSubject("이메일 인증 코드");
    message.setText("인증 코드: " + code + "\n10분 안에 입력해주세요.");
    mailSender.send(message);
  }

  public boolean verifyCode(String email, String code) {
    return verificationRepository.findByEmailAndCodeAndUsedFalse(email, code)
        .map(verification -> {
          if (verification.getExpireAt().isBefore(LocalDateTime.now()))
            return false;
          verification.setUsed(true);
          verificationRepository.save(verification);
          // 유저 활성화
          userRepository.findByEmail(email).ifPresent(u -> {
            u.setActive(true);
            userRepository.save(u);
          });
          return true;
        }).orElse(false);
  }

  private String generateCode() {
    int code = new SecureRandom().nextInt(900000) + 100000;
    return String.valueOf(code);
  }
}