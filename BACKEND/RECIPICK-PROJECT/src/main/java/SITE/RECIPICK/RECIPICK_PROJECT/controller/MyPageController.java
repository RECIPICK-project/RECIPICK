package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.MyProfileResponse;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.NicknameUpdateRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.service.MyPageService;
import SITE.RECIPICK.RECIPICK_PROJECT.util.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 마이페이지 컨트롤러
 * <p>
 * 역할 - 현재 로그인 사용자의 마이페이지 관련 API 집합
 * <p>
 * 인증 - 사용자 식별자는 SecurityContext에서 가져온다(AuthUtil)
 * <p>
 * 예외 처리 - 현재는 런타임 예외가 그대로 전달될 수 있음. 운영 전 @ControllerAdvice 로 400/403/404/409 등의 에러 포맷 표준화 권장.
 */
@RestController
@RequestMapping("/me") // 모든 API 경로는 /me 로 시작
@Tag(name = "My Page", description = "마이페이지: 프로필 조회/수정 APIs")
public class MyPageController {

  private final MyPageService myPageService;

  public MyPageController(MyPageService myPageService) {
    this.myPageService = myPageService;
  }

  /**
   * 프로필 조회
   * <p>
   * 반환 필드 - 닉네임, 등급, 프로필 이미지 - 내가 올린 정식 레시피 개수 - 내 정식 레시피의 총 좋아요 수 - 활동 수(리뷰 + 댓글)
   */
  @GetMapping("/profile")
  @Operation(
      summary = "내 프로필 조회",
      description = """
          현재 사용자 프로필 정보를 반환합니다.
          - 닉네임, 등급, 프로필 이미지
          - 내가 올린 정식 레시피 개수
          - 내 정식 레시피의 총 좋아요 수
          - 활동 수(리뷰 + 댓글)
          """
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "성공",
          content = @Content(schema = @Schema(implementation = MyProfileResponse.class))
      ),
      @ApiResponse(
          responseCode = "404",
          description = "프로필 없음(PROFILE_NOT_FOUND) — 테스트 계정 세팅 전일 수 있음",
          content = @Content
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 오류",
          content = @Content
      )
  })
  public MyProfileResponse getProfile() {
    Integer userId = AuthUtil.getLoginUserId(); // ← 로그인 사용자 ID
    return myPageService.getMyProfile(userId);
  }

  /**
   * 닉네임 변경(7일 제한)
   * <p>
   * 규칙 - 공백/길이(≤50) 검증 - 기존 닉네임과 동일이면 변경 없음 - updated_at 기준 7일 이내면 거부 (NICKNAME_CHANGE_COOLDOWN) -
   * 닉네임 중복이면 거부 (NICKNAME_DUPLICATED)
   * <p>
   * 성공 시 204(No Content) 반환
   */
  @PatchMapping("/profile/nickname")
  @Operation(
      summary = "닉네임 변경(7일 제한)",
      description = """
          닉네임을 변경합니다.
          - 7일 쿨다운 적용(updated_at 기준)
          - 중복 닉네임 방지
          - 성공 시 본문 없는 204(No Content)
          """
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "변경 성공"),
      @ApiResponse(responseCode = "400", description = "요청 값 오류(NICKNAME_REQUIRED/NICKNAME_TOO_LONG 등)"),
      @ApiResponse(responseCode = "409", description = "쿨다운 또는 중복(NICKNAME_CHANGE_COOLDOWN / NICKNAME_DUPLICATED)"),
      @ApiResponse(responseCode = "404", description = "프로필 없음(PROFILE_NOT_FOUND)"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  public ResponseEntity<Void> changeNickname(
      @Parameter(description = "변경할 닉네임", required = true)
      @RequestBody NicknameUpdateRequest req
  ) {
    Integer userId = AuthUtil.getLoginUserId(); // ← 로그인 사용자 ID
    myPageService.changeNickname(userId, req);
    return ResponseEntity.noContent().build();
  }
}
