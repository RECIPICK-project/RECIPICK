package SITE.RECIPICK.RECIPICK_PROJECT.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponseDto {

  private Integer reviewId;
  private Integer postId;
  private Integer userId;
  private String nickname;
  private BigDecimal rating;
  private String comment;
  private int reportCount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}