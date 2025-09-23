package SITE.RECIPICK.RECIPICK_PROJECT.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.EmailVerification;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository repository;
    private final JavaMailSender mailSender;

    // 정책
    private static final int EXPIRE_MINUTES = 10;         // 코드 유효 시간
    private static final int MAX_ATTEMPTS   = 5;          // 최대 시도 횟수
    private static final int DAILY_LIMIT    = 5;          // 하루 발송 횟수
    private static final Duration COOLDOWN  = Duration.ofSeconds(60); // 발송 쿨다운

    private static final SecureRandom RND = new SecureRandom();

    // 발송 / 재전송
    @Transactional
    public void sendVerificationCode(String email) {
        email = norm(email);
        LocalDateTime now = LocalDateTime.now();

        // 가장 최근 기록
        Optional<EmailVerification> latestOpt =
            repository.findTopByEmailOrderByCreatedAtDesc(email);

        if (latestOpt.isPresent()) {
            EmailVerification latest = latestOpt.get();

            // 쿨다운
            if (latest.getLastSentAt() != null
                && Duration.between(latest.getLastSentAt(), now).compareTo(COOLDOWN) < 0) {
                throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "요청이 너무 잦습니다. 잠시 후 다시 시도하세요."
                );
            }

            // 금일 발송 횟수
            int todayCount =
                (latest.getLastSentDate() != null && latest.getLastSentDate().equals(LocalDate.now()))
                    ? (latest.getSendCountToday() == null ? 0 : latest.getSendCountToday())
                    : 0;

            if (todayCount >= DAILY_LIMIT) {
                throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "금일 발송 한도를 초과했습니다."
                );
            }
        }

        // 새 코드 생성
        String code = generateCode();
        String hash = sha256(code);

        // 새 레코드 저장 (금일 횟수 누적)
        EmailVerification entity = EmailVerification.builder()
            .email(email)
            .codeHash(hash)
            .expireAt(now.plusMinutes(EXPIRE_MINUTES))
            .used(false)
            .attempts(0)
            .lastSentAt(now)
            .lastSentDate(LocalDate.now())
            .sendCountToday(
                latestOpt.map(latest -> {
                    if (latest.getLastSentDate() != null && latest.getLastSentDate().equals(LocalDate.now())) {
                        return (latest.getSendCountToday() == null ? 0 : latest.getSendCountToday()) + 1;
                    } else {
                        return 1;
                    }
                }).orElse(1)
            )
            .build();

        repository.save(entity);

        // 이메일 발송
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject("[Recipe] 이메일 인증 코드");
        msg.setText("인증 코드: " + code + "\n유효 시간: " + EXPIRE_MINUTES + "분");

        try {
            mailSender.send(msg);
        } catch (org.springframework.mail.MailException ex) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "메일 전송에 실패했습니다. 잠시 후 다시 시도하세요.",
                ex
            );
        }
    } // ✅ 메서드 여기서 정확히 닫힘

    // 코드 검증
    @Transactional
    public boolean verifyCode(String email, String inputCode) {
        email = norm(email);

        Optional<EmailVerification> latestOpt =
            repository.findTopByEmailOrderByCreatedAtDesc(email);
        if (latestOpt.isEmpty()) return false;

        EmailVerification v = latestOpt.get();
        LocalDateTime now = LocalDateTime.now();

        if (v.isUsed()) return false;
        if (now.isAfter(v.getExpireAt())) return false;
        if (v.getAttempts() >= MAX_ATTEMPTS) return false;

        String inputHash = sha256(inputCode);
        if (inputHash.equalsIgnoreCase(v.getCodeHash())) {
            v.setUsed(true);
            v.setUsedAt(now);
            repository.save(v);
            return true;
        } else {
            v.setAttempts(v.getAttempts() + 1);
            repository.save(v);
            return false;
        }
    }

    // 이메일 인증 여부
    public boolean isEmailVerified(String email) {
        email = norm(email);
        return repository.existsByEmailAndUsedTrue(email);
    }

    // 6자리 코드
    private String generateCode() {
        return String.format("%06d", RND.nextInt(1_000_000));
    }

    // SHA-256
    private String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : dig) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 이메일 정규화
    private String norm(String s) {
        return s == null ? null : s.trim().toLowerCase();
    }
}
