package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.ProfileImageUpdateRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.service.AvatarPresignService;
import SITE.RECIPICK.RECIPICK_PROJECT.service.ProfileCommandService;
import SITE.RECIPICK.RECIPICK_PROJECT.util.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
<<<<<<< HEAD
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
=======
>>>>>>> 82b80b20265352b2dd34d77d29951c197c9966b5

/**
 * ProfileCommandController
 *
 * <p>✅ 역할 - 사용자의 프로필 변경(명령형 작업)을 처리하는 API 컨트롤러 - 조회 전용은 MyPageController가 담당, 변경/갱신은 이 컨트롤러가 담당
 *
 * <p>✅ 엔드포인트 - PATCH /me/profile/image → 프로필 이미지 변경
 *
 * <p>
 */
@RestController
@RequestMapping("/me/profile")
@Tag(name = "My Profile (Command)", description = "프로필 변경 API")
public class ProfileCommandController {

<<<<<<< HEAD
  private final ProfileCommandService svc; // 프로필 변경 로직 담당 서비스
  private final CurrentUser currentUser;
  private final AvatarPresignService Presign;


  public ProfileCommandController(ProfileCommandService svc, CurrentUser currentUser,
      AvatarPresignService presign) {
    this.svc = svc;
    this.currentUser = currentUser;
    this.Presign = presign;
  }
=======
    private final ProfileCommandService svc; // 프로필 변경 로직 담당 서비스
    private final CurrentUser currentUser;

    public ProfileCommandController(ProfileCommandService svc, CurrentUser currentUser) {
        this.svc = svc;
        this.currentUser = currentUser;
    }
>>>>>>> 82b80b20265352b2dd34d77d29951c197c9966b5

    /**
     * 프로필 이미지 변경
     *
     * @param req ProfileImageUpdateRequest (JSON Body)
     *     <p>요청 JSON 예시: { "profileImg": "/img/me-new.png" }
     *     <p>검증 규칙: - profileImg null/빈 값 → IllegalArgumentException("PROFILE_IMG_REQUIRED")
     *     <p>성공 시: - DB의 프로필 이미지 URL 업데이트 - updated_at 타임스탬프 갱신 - 응답 코드: 204 No Content
     */
    @PatchMapping("/image")
    @Operation(summary = "프로필 이미지 변경", description = "현재 로그인 사용자의 프로필 이미지를 변경합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "변경 성공 (본문 없음)"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (PROFILE_IMG_REQUIRED)"),
        @ApiResponse(responseCode = "404", description = "프로필 없음 (PROFILE_NOT_FOUND)"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public void changeImage(@RequestBody ProfileImageUpdateRequest req) {
        // ✅ 배포용: SecurityContext에서 userId 가져오기
        Integer userId = currentUser.userId();

<<<<<<< HEAD
    svc.changeProfileImage(userId, req);
  }

  /**
   * 아바타 URL 저장 Body: { "profileImg": "https://..." }
   */
  @PatchMapping(value = "/avatar", consumes = "application/json")
  @Operation(summary = "프로필 이미지 URL 저장")
  public ResponseEntity<?> changeProfileImage(@RequestBody ProfileImageUpdateRequest req) {
    Integer me = currentUser.userId();
    svc.changeProfileImage(me, req);
    // 본문 없이 204
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/avatar/presign")
  @Operation(summary = "아바타 업로드용 S3 PUT Presigned URL 발급")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "성공(putUrl, publicUrl 반환)")
  })
  public ResponseEntity<?> presignAvatar(
      @RequestParam String filename,
      @RequestParam(required = false) String contentType
  ) {
    Integer me = currentUser.userId();
    return ResponseEntity.ok(Presign.createPutUrl(me, filename, contentType));
  }
=======
        svc.changeProfileImage(userId, req);
    }
>>>>>>> 82b80b20265352b2dd34d77d29951c197c9966b5
}
