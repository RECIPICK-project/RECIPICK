package SITE.RECIPICK.RECIPICK_PROJECT.service;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PasswordChangeRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.UserEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비밀번호 변경 관련 서비스 계층
 * <p>
 * 컨트롤러(PasswordController)에서 호출되며, - 기존 비밀번호 확인 - 비밀번호 유효성 검사 - 새 비밀번호 암호화 후 저장 을 담당한다.
 */
@Service
@RequiredArgsConstructor // final 필드 기반 생성자 자동 생성
public class PasswordService {

  private final UserRepository userRepo;   // 사용자 조회/저장용 Repository
  private final PasswordEncoder encoder;   // Spring Security 제공 비밀번호 암호화기

  /**
   * 비밀번호 변경 로직
   *
   * @param me  현재 로그인한 사용자 ID
   * @param req 클라이언트가 보낸 변경 요청 DTO (oldPassword, newPassword)
   *            <p>
   *            1) 요청 값 검증 - oldPassword / newPassword 둘 다 null이면 거부
   *            <p>
   *            2) 사용자 조회 - userRepo.findById(me) - 없으면 USER_NOT_FOUND 예외
   *            <p>
   *            3) 기존 비밀번호 확인 - 저장된 값이 bcrypt 해시이면 encoder.matches()로 비교 - 저장된 값이 평문일 수도 있으니 평문 비교도
   *            지원 → 평문이 일치하면 즉시 bcrypt로 마이그레이션
   *            <p>
   *            4) 비밀번호 정책 검사 - 새 비밀번호 길이 8자 미만 → WEAK_PASSWORD - 새 비밀번호가 기존 비밀번호와 동일 →
   *            PASSWORD_SAME_AS_OLD
   *            <p>
   *            5) 최종 저장 - 새 비밀번호를 bcrypt로 인코딩하여 User 엔티티에 저장 - @Transactional + dirty checking →
   *            UPDATE 자동 반영
   *            <p>
   *            발생 가능한 예외: - IllegalArgumentException("PASSWORD_REQUIRED") → 입력 누락 -
   *            IllegalArgumentException("USER_NOT_FOUND") → 사용자 없음 -
   *            IllegalStateException("OLD_PASSWORD_MISMATCH") → 기존 비번 불일치 -
   *            IllegalArgumentException("WEAK_PASSWORD") → 새 비번 너무 짧음 -
   *            IllegalArgumentException("PASSWORD_SAME_AS_OLD") → 새/구 동일
   */
  @Transactional
  public void changePassword(Integer me, PasswordChangeRequest req) {
    // 1. 요청 값 검증
    if (req.getOldPassword() == null || req.getNewPassword() == null) {
      throw new IllegalArgumentException("PASSWORD_REQUIRED");
    }

    // 2. 사용자 조회
    UserEntity u = userRepo.findById(me)
        .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

    String stored = u.getPassword();
    boolean isBcrypt = stored != null && stored.startsWith("$2"); // bcrypt 해시 여부 확인

    // 3. 기존 비밀번호 확인
    boolean oldMatches;
    if (isBcrypt) {
      oldMatches = encoder.matches(req.getOldPassword(), stored);
    } else {
      // 초기 평문 상태 지원
      oldMatches = req.getOldPassword().equals(stored);
      if (oldMatches) {
        // 평문 → bcrypt로 즉시 마이그레이션
        u.setPassword(encoder.encode(stored));
      }
    }
    if (!oldMatches) {
      throw new IllegalStateException("OLD_PASSWORD_MISMATCH");
    }

    // 4. 비밀번호 정책 검사
    if (req.getNewPassword().length() < 8) {
      throw new IllegalArgumentException("WEAK_PASSWORD");
    }
    if (req.getNewPassword().equals(req.getOldPassword())) {
      throw new IllegalArgumentException("PASSWORD_SAME_AS_OLD");
    }

    // 5. 새 비밀번호 저장 (bcrypt 인코딩)
    u.setPassword(encoder.encode(req.getNewPassword()));
  }

}
