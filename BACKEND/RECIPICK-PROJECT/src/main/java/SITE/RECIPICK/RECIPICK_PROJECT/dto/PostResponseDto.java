package SITE.RECIPICK.RECIPICK_PROJECT.dto;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostResponseDto {

  private Integer postId;
  private String title;
  private String foodName;
  private String rcpImgUrl;
  private int viewCount;
  private int likeCount;

  public static PostResponseDto fromEntity(Post post) {
    return new PostResponseDto(
        post.getPostId(),
        post.getTitle(),
        post.getFoodName(),
        post.getRcpStepsImg() != null ? post.getRcpStepsImg() : "", // null 처리
        post.getViewCount(),
        post.getLikeCount()
    );
  }
}
