package SITE.RECIPICK.RECIPICK_PROJECT.repository;


import SITE.RECIPICK.RECIPICK_PROJECT.entity.EmailVerification;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

  // 기존
  Optional<EmailVerification> findTopByEmailOrderByCreatedAtDesc(String email);
  boolean existsByEmailAndUsedTrue(String email);

  // (선택) 사용되지 않은 최신 건만 보고 싶을 때
  Optional<EmailVerification> findTopByEmailAndUsedFalseOrderByCreatedAtDesc(String email);

  // 일일 발송 횟수 계산용 (쿨다운/일일 한도 로직에 유용)
  long countByEmailAndLastSentDate(String email, LocalDate lastSentDate);

  // 정리 작업용: 만료되었거나 오래 전에 사용된 레코드 삭제
  @Modifying
  @Query("delete from EmailVerification v where v.expireAt < :now")
  int deleteAllExpired(LocalDateTime now);

  @Modifying
  @Query("delete from EmailVerification v where v.used = true and v.usedAt < :before")
  int deleteAllUsedBefore(LocalDateTime before);

  // (선택) 시도 횟수 +1 (동시성 줄이고 싶을 때)
  @Modifying
  @Query("update EmailVerification v set v.attempts = v.attempts + 1 where v.id = :id")
  int incrementAttempts(Long id);
}