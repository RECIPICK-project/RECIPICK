package group3.recipe.repository;

import group3.recipe.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Integer> {

  // 사용되지 않은 이메일 인증 코드 조회
  Optional<EmailVerification> findByEmailAndCodeAndUsedFalse(String email, String code);

  // 이미 사용된 이메일 존재 여부 확인
  boolean existsByEmailAndUsedTrue(String email);

}