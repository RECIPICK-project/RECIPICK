package SITE.RECIPICK.RECIPICK_PROJECT.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MyProfileResponse: /me/profile API의 응답을 담는 DTO 클래스.
 *
 * <p>Lombok을 이용해 getter/setter, 생성자 등을 자동 생성한다.
 */
@Data
@AllArgsConstructor // 모든 필드를 받는 생성자 생성
@NoArgsConstructor // 파라미터 없는 기본 생성자 생성
@Schema(description = "내 프로필 응답 DTO")
public class MyProfileResponse {

  // 닉네임 (PROFILE.nickname)
  private String nickname;

  // 회원 등급 (PROFILE.grade)
  private String grade;

  // 프로필 이미지 URL (PROFILE.profile_img)
  private String profileImg;

  // 내가 올린 정식 레시피 개수 (POST.rcp_is_official=1인 글 count)
  private long myRecipeCount;

  // 내 정식 레시피들이 받은 좋아요 합계 (POST.like_count 합계)
  private long totalLikesOnMyPosts;

  // 내가 남긴 활동 수 = 내가 쓴 댓글 수 + 내가 쓴 리뷰(별점) 수
  private long activityCount;

  private String role;

}
