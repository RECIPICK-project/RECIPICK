package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.ProfileImageUpdateRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.service.ProfileCommandService;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프로필 관련 변경(명령) API 컨트롤러 - 조회 전용은 MyPageController에서 담당 - 변경/갱신은 여기서 처리
 */
@RestController
@RequestMapping("/me/profile")
public class ProfileCommandController {

  // 아직 로그인/인증이 붙지 않았기 때문에, 임시로 userId=1번으로 고정
  private static final Integer ME = 1;

  // 실제 비즈니스 로직은 서비스 계층이 담당
  private final ProfileCommandService svc;

  public ProfileCommandController(ProfileCommandService svc) {
    this.svc = svc;
  }

  /**
   * 프로필 이미지 변경 PATCH /me/profile/image
   * <p>
   * 요청 바디: ProfileImageUpdateRequest (JSON) { "profileImgUrl": "/img/me-new.png" }
   * <p>
   */
  @PatchMapping("/image")
  public void changeImage(@RequestBody ProfileImageUpdateRequest req) {
    svc.changeProfileImage(ME, req);
  }
}
