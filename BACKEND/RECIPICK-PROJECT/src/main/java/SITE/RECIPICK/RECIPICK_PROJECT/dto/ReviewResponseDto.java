package SITE.RECIPICK.RECIPICK_PROJECT.dto;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.ReviewEntity;
import java.math.BigDecimal;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class ReviewResponseDto {

  private Long reviewId;
  private Integer postId;
  private Integer userId;
  private BigDecimal rating;
  private String comment;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public ReviewResponseDto(ReviewEntity review) {
    this.reviewId = review.getId();
    this.postId = review.getPost().getPostId();
    this.userId = review.getUser().getUserId(); // Assuming UserTestEntity has a getId() or getUserId() method
    this.rating = review.getRating();
    this.comment = review.getComment();
    this.createdAt = review.getCreatedAt();
    this.updatedAt = review.getUpdatedAt();
  }
}