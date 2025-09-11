package SITE.RECIPICK.RECIPICK_PROJECT.dto;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.ReviewEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class ReviewResponseDto {

  private final Integer reviewId;
  private final Integer postId;
  private final Integer userId;
  private final BigDecimal rating;
  private final String comment;
  private final LocalDateTime createdAt;
  private final LocalDateTime updatedAt;

  public ReviewResponseDto(ReviewEntity review) {
    this.reviewId = review.getId();
    this.postId = review.getPost().getPostId();
    this.userId = review.getUser()
        .getUserId(); // Assuming UserTestEntity has a getId() or getUserId() method
    this.rating = review.getRating();
    this.comment = review.getComment();
    this.createdAt = review.getCreatedAt();
    this.updatedAt = review.getUpdatedAt();
  }
}