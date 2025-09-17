package SITE.RECIPICK.RECIPICK_PROJECT.dto;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.ReviewEntity;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL) // null 값 제외하고 JSON 직렬화
public class ReviewDto {

  // === 기본 리뷰 정보 ===
  private Integer reviewId;
  private Integer postId;
  private Integer userId;
  private String nickname;
  private BigDecimal rating;
  private String comment;
  private Integer reportCount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // === 통계 정보 (리뷰 통계 조회 시에만 사용) ===
  private BigDecimal averageRating;
  private Long totalReviews;
  private Long ratingCount1;
  private Long ratingCount2;
  private Long ratingCount3;
  private Long ratingCount4;
  private Long ratingCount5;

  // === 생성자들 ===

  // 리뷰 생성/수정 요청용 생성자
  public ReviewDto(Integer postId, BigDecimal rating, String comment) {
    this.postId = postId;
    this.rating = rating;
    this.comment = comment;
  }

  // 리뷰 응답용 생성자 (Entity → DTO 변환용)
  public ReviewDto(Integer reviewId, Integer postId, Integer userId, String nickname,
      BigDecimal rating, String comment, Integer reportCount,
      LocalDateTime createdAt, LocalDateTime updatedAt) {
    this.reviewId = reviewId;
    this.postId = postId;
    this.userId = userId;
    this.nickname = nickname;
    this.rating = rating;
    this.comment = comment;
    this.reportCount = reportCount;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public ReviewDto(BigDecimal averageRating, Long totalReviews,
      Long ratingCount1, Long ratingCount2, Long ratingCount3,
      Long ratingCount4, Long ratingCount5) {
    this.averageRating = averageRating;
    this.totalReviews = totalReviews;
    this.ratingCount1 = ratingCount1;
    this.ratingCount2 = ratingCount2;
    this.ratingCount3 = ratingCount3;
    this.ratingCount4 = ratingCount4;
    this.ratingCount5 = ratingCount5;
  }

  public static ReviewDto fromEntity(ReviewEntity entity) {
    return ReviewDto.builder()
        .reviewId(entity.getReviewId())
        .postId(entity.getPost().getPostId())
        .userId(entity.getUser().getUserId())
        .nickname(entity.getUser().getNickname()) // User entity에서 가져옴
        .rating(entity.getReviewRating())
        .comment(entity.getComment())
        .reportCount(entity.getReportCount())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }

  // 통계 전용 DTO 생성
  public static ReviewDto forStats(BigDecimal avgRating, Long totalReviews,
      Long count1, Long count2, Long count3,
      Long count4, Long count5) {
    return ReviewDto.builder()
        .averageRating(avgRating)
        .totalReviews(totalReviews)
        .ratingCount1(count1)
        .ratingCount2(count2)
        .ratingCount3(count3)
        .ratingCount4(count4)
        .ratingCount5(count5)
        .build();
  }

  // 요청 전용 DTO 생성
  public static ReviewDto forRequest(Integer postId, BigDecimal rating, String comment) {
    return ReviewDto.builder()
        .postId(postId)
        .rating(rating)
        .comment(comment)
        .build();
  }
}