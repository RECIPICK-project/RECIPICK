package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PasswordChangeRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.service.PasswordService;
import SITE.RECIPICK.RECIPICK_PROJECT.util.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PasswordController
 * <p>
 * ✅ 역할 - 현재 로그인 사용자의 "비밀번호 변경" API 담당 컨트롤러 - /me/profile/password 엔드포인트 제공
 * <p>
 * ✅ 특징 - PATCH 요청으로 oldPassword, newPassword를 JSON Body로 받음 - 서비스 계층(PasswordService)에서 검증 및 암호화
 * 업데이트 처리 - oldPassword 불일치 시 "OLD_PASSWORD_MISMATCH" 예외 발생 - 성공 시 204 No Content 반환
 * <p>
 * ⚠️ 주의 - 개발 중에는 하드코딩된 userId를 사용했지만, 배포 시에는 반드시 SecurityContext → CurrentUser 유틸로 대체해야 함.
 */
@RestController
@RequestMapping("/me/profile")
@Tag(name = "My Profile (Password)", description = "내 프로필 비밀번호 변경 API")
public class PasswordController {

  private final PasswordService svc; // 비밀번호 변경 로직 담당 서비스

  public PasswordController(PasswordService svc) {
    this.svc = svc;
  }

  /**
   * 비밀번호 변경 API
   *
   * @param req PasswordChangeRequest (oldPassword, newPassword)
   *            <p>
   *            요청 JSON 예시: { "oldPassword": "현재비번", "newPassword": "새비번" }
   *            <p>
   *            검증 규칙: - oldPassword 불일치 → IllegalStateException("OLD_PASSWORD_MISMATCH") -
   *            newPassword null/공백 → IllegalArgumentException("PASSWORD_REQUIRED") - newPassword와
   *            oldPassword 동일 → IllegalArgumentException("PASSWORD_SAME_AS_OLD") - newPassword 길이 <
   *            8 → IllegalArgumentException("WEAK_PASSWORD")
   *            <p>
   *            성공 시: - DB에 bcrypt로 암호화된 비밀번호 저장 - 응답코드: 204 No Content
   */
  @PatchMapping("/password")
  @Operation(
      summary = "비밀번호 변경",
      description = """
          내 프로필 비밀번호를 변경합니다.
          - oldPassword 검증 후 newPassword로 교체
          - bcrypt 암호화 저장
          - 실패 시 400/409 에러 코드 반환
          """
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "비밀번호 변경 성공 (본문 없음)"),
      @ApiResponse(responseCode = "400", description = "입력값 오류 (PASSWORD_REQUIRED, WEAK_PASSWORD 등)"),
      @ApiResponse(responseCode = "409", description = "기존 비밀번호 불일치 (OLD_PASSWORD_MISMATCH)")
  })
  public void changePassword(@RequestBody PasswordChangeRequest req) {
    // ✅ 배포용: SecurityContext에서 로그인한 사용자 ID 추출
    Integer userId = CurrentUser.currentUserId();

    svc.changePassword(userId, req);
  }
}
