package SITE.RECIPICK.RECIPICK_PROJECT.controller.admin;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.GradeUpdateRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.service.admin.AdminGradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AdminGradeController
 * <p>
 * ✅ 목적 - 관리자 전용 "유저 등급(Grade) 관리" API 전담 컨트롤러. - URL prefix: /admin/users
 * <p>
 * ✅ 보안 - 모든 엔드포인트는 ROLE_ADMIN 권한 필요(@PreAuthorize). - SecurityConfig 에서 /admin/** 요청을
 * hasRole('ADMIN') 으로 제한해야 함.
 * <p>
 * ✅ 연동 - AdminGradeService.updateUserGrade(userId, req) 에 도메인 검증/변경 로직 위임.
 * <p>
 * ✅ 요청/응답 규격 - PATCH /admin/users/{userId}/grade - Path: userId (Integer) : 등급을 변경할 대상 사용자 ID -
 * Body: { "grade": "BRONZE|SILVER|GOLD" } - 204 No Content (성공) - 400 Bad Request : 잘못된 grade 값(허용
 * enum 외), 누락된 필드 등 - 404 Not Found   : 대상 사용자/프로필 없음 (서비스에서 던진 예외를 글로벌 핸들러가 404로 매핑 권장) - 409
 * Conflict    : 비즈니스 충돌(예: 이미 동일 등급 등) — 필요 시 서비스에서 명확한 에러코드로 throw
 * <p>
 * ✅ 예외 처리(권장) - @ControllerAdvice(GlobalExceptionHandler)에서 IllegalArgumentException → 400
 * NoSuchElementException   → 404 IllegalStateException    → 409 로 매핑하면 Swagger 에서도 깔끔하게 확인 가능.
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminGradeController {

  // 도메인 로직(등급 변경) 위임 대상 서비스
  private final AdminGradeService svc;

  /**
   * 유저 등급 변경
   * <p>
   * 예) PATCH /admin/users/12/grade Body: { "grade": "SILVER" }
   * <p>
   * 보안: ROLE_ADMIN 필수
   */
  @PatchMapping("/{userId}/grade")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> changeGrade(
      @PathVariable Integer userId,
      @RequestBody GradeUpdateRequest req
  ) {
    // 서비스에서 grade 유효성(BRONZE|SILVER|GOLD), 대상 존재 여부, 업데이트 수행
    svc.updateUserGrade(userId, req);
    // 본문 없이 성공만 알림
    return ResponseEntity.noContent().build();
  }
}
