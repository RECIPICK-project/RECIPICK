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
 * ✅ 관리자 전용 "유저 등급 관리" API 전담 컨트롤러 - /admin/users/{userId}/grade 경로 제공 - AdminGradeService 와 연결
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminGradeController {

  private final AdminGradeService svc; // 유저 등급 변경 서비스

  /**
   * 유저 등급 변경
   * <p>
   * - PathVariable: userId → 변경할 사용자 ID - RequestBody: GradeUpdateRequest → 변경할 등급 (BRONZE | SILVER
   * | GOLD) - 권한: 관리자 전용 (ROLE_ADMIN 필요) - 응답: 성공 시 204 No Content
   */
  @PatchMapping("/{userId}/grade")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> changeGrade(@PathVariable Integer userId,
      @RequestBody GradeUpdateRequest req) {
    svc.updateUserGrade(userId, req);
    return ResponseEntity.noContent().build();
  }
}
