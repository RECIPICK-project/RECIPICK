package SITE.RECIPICK.RECIPICK_PROJECT.dto.search;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class SearchPostDto {

  private Long postId;
  private Integer userId;
  private String title;
  private String foodName;
  private String rcpImgUrl;
  private String rcpStepsImg;
  private Integer viewCount;
  private Integer likeCount;
  private LocalDateTime createdAt;
  private Integer subScore; // 선택적 계산용 필드

  // 기본 생성자
  public SearchPostDto() {
  }

}
