package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PasswordChangeRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.service.PasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내 프로필 - 비밀번호 변경 컨트롤러
 * <p>
 * 특징: - PATCH /me/profile/password - JSON Body로 oldPassword, newPassword 전달 - oldPassword 불일치 시 예외
 * 발생 (OLD_PASSWORD_MISMATCH) - 성공 시 204 No Content 반환
 */
@RestController
@RequestMapping("/me/profile")
@Tag(name = "My Profile (Password)", description = "내 프로필 비밀번호 변경 API")
public class PasswordController {

  // 로그인/보안 연동 전, userId=1로 임시 고정
  private static final Integer ME = 1;

  private final PasswordService svc;

  public PasswordController(PasswordService svc) {
    this.svc = svc;
  }

  /**
   * 비밀번호 변경
   * <p>
   * 요청 JSON 예시: { "oldPassword": "현재비번", "newPassword": "새비번" }
   * <p>
   * 규칙: - oldPassword 불일치 → IllegalStateException("OLD_PASSWORD_MISMATCH") - newPassword null/공백 →
   * IllegalArgumentException("PASSWORD_REQUIRED") - 성공 → DB 비밀번호 bcrypt 업데이트
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
      @ApiResponse(responseCode = "204", description = "비밀번호 변경 성공 (내용 없음)"),
      @ApiResponse(responseCode = "400", description = "입력값 오류 (예: 비밀번호 null/공백)"),
      @ApiResponse(responseCode = "409", description = "기존 비밀번호 불일치 (OLD_PASSWORD_MISMATCH)")
  })
  public void changePassword(@RequestBody PasswordChangeRequest req) {
    svc.changePassword(ME, req);
  }
}
