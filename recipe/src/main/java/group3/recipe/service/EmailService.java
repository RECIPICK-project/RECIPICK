package group3.recipe.service;

import group3.recipe.entity.EmailVerification;
import group3.recipe.repository.EmailVerificationRepository;
import group3.recipe.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailService {

  private final EmailVerificationRepository verificationRepository;
  private final JavaMailSender mailSender;
  private final UserRepository userRepository;

  private String norm(String email) {
    return email == null ? null : email.trim().toLowerCase();
  }

  private String sha256(String s) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (Exception e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  private String generateCode() {
    // 6자리 숫자
    return String.valueOf(new SecureRandom().nextInt(900000) + 100000);
  }

  /** 인증 코드 발송 (DB에는 해시만 저장) */
  @Transactional
  public void sendVerificationCode(String rawEmail) {
    String email = norm(rawEmail);
    String code = generateCode();
    String codeHash = sha256(code);
    LocalDateTime expireAt = LocalDateTime.now().plusMinutes(10);

    verificationRepository.save(
        EmailVerification.builder()
            .email(email)
            .codeHash(codeHash)     // ✅ code → codeHash
            .expireAt(expireAt)
            .used(false)
            .build()
    );

    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(email);
    message.setSubject("[Recipe] 이메일 인증 코드");
    message.setText("인증 코드: " + code + "\n10분 안에 입력해주세요.");
    mailSender.send(message);
  }

  /** 코드 검증 (가장 최근 레코드 기준) */
  @Transactional
  public boolean verifyCode(String rawEmail, String inputCode) {
    String email = norm(rawEmail);
    String inputHash = sha256(inputCode);

    Optional<EmailVerification> latestOpt =
        verificationRepository.findTopByEmailOrderByCreatedAtDesc(email);

    if (latestOpt.isEmpty()) return false;

    EmailVerification v = latestOpt.get();

    // 만료 또는 이미 사용됨
    if (v.isUsed()) return false;
    if (v.getExpireAt() != null && v.getExpireAt().isBefore(LocalDateTime.now())) return false;

    // 해시 비교
    if (!inputHash.equalsIgnoreCase(v.getCodeHash())) return false;

    // 사용 처리
    v.setUsed(true);
    verificationRepository.save(v);

    // (정책) 인증되면 계정 활성화
    userRepository.findByEmail(email).ifPresent(u -> {
      u.setActive(true);
      userRepository.save(u);
    });

    return true;
  }
}