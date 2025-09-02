package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.MyProfileResponse;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.NicknameUpdateRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.service.MyPageService;
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
 * 역할 - 현재 로그인 사용자의 마이페이지 관련 API 집합 - 아직 인증 미연동 상태라 임시 사용자 ID(ME=1)로 동작
 * <p>
 * 주의 - 추후 Spring Security/JWT를 붙이면 ME 상수 제거하고 SecurityContext에서 userId를 꺼내야 함. - 예외 응답 표준화는
 * @ControllerAdvice로 400/403/404/409 등 통일 권장(현재는 런타임 예외 그대로 전달될 수 있음).
 */
@RestController // 이 클래스를 REST 컨트롤러로 등록(리턴값을 JSON으로 직렬화)
@RequestMapping("/me") // 모든 API 경로는 /me 로 시작
@Tag(name = "My Page", description = "마이페이지: 프로필 조회/수정 APIs")
public class MyPageController {

  // 로그인/인증 붙이기 전 임시 사용자 식별자
  private static final Integer ME = 1;

  // 컨트롤러는 요청 위임만; 실제 로직은 서비스가 담당
  private final MyPageService myPageService;

  public MyPageController(MyPageService myPageService) {
    this.myPageService = myPageService;
  }

  /**
   * 프로필 조회
   * <p>
   * 반환: 닉네임, 등급, 프로필 이미지, 내가 올린 정식 레시피 개수, 총 좋아요 수, 활동 수(리뷰+댓글)
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
          content = @Content // 에러 바디 표준화 전이라 스키마 생략
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 오류",
          content = @Content
      )
  })
  public MyProfileResponse getProfile() {
    // 임시 userId(ME=1). 추후 SecurityContext에서 가져오도록 교체.
    return myPageService.getMyProfile(ME);
  }

  /**
   * 닉네임 변경(7일 제한)
   * <p>
   * 규칙: - 공백/길이(≤50) 검증 - 기존 닉네임과 동일이면 변경 없음 - updated_at 기준 7일 이내면 거부 (NICKNAME_CHANGE_COOLDOWN) -
   * 닉네임 중복이면 거부 (NICKNAME_DUPLICATED) 성공 시 204(No Content)
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
    myPageService.changeNickname(ME, req);
    return ResponseEntity.noContent().build();
  }
}


