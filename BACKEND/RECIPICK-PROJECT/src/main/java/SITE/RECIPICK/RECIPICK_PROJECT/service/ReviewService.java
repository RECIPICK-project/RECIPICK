package SITE.RECIPICK.RECIPICK_PROJECT.service;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.ReviewRequestDto;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.ReviewResponseDto;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.ReviewStatsDto;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.ReviewEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.UserEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.PostRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.ReviewRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final PostRepository postRepository;
  private final UserRepository userRepository;

  /**
   * 리뷰 작성
   */
  @Transactional
  public ReviewResponseDto createReview(Integer userId, ReviewRequestDto requestDto) {
    if (reviewRepository.existsByPostPostIdAndUserUserId(requestDto.getPostId(), userId)) {
      throw new IllegalArgumentException("이미 이 레시피에 리뷰를 작성하셨습니다.");
    }

    PostEntity post = postRepository.findById(requestDto.getPostId())
        .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

    UserEntity user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

    if (requestDto.getRating().compareTo(BigDecimal.ZERO) < 0 ||
        requestDto.getRating().compareTo(BigDecimal.valueOf(5)) > 0) {
      throw new IllegalArgumentException("평점은 0.0 ~ 5.0 사이여야 합니다.");
    }

    ReviewEntity review = ReviewEntity.builder()
        .post(post)
        .user(user)
        .rating(requestDto.getRating())
        .comment(requestDto.getComment())
        .build();

    ReviewEntity savedReview = reviewRepository.save(review);

    return convertToResponseDto(savedReview);
  }

  /**
   * 특정 게시글의 리뷰 목록 조회
   */
  public Page<ReviewResponseDto> getReviewsByPostId(Integer postId, Pageable pageable) {
    Page<ReviewEntity> reviews = reviewRepository.findByPostPostIdOrderByCreatedAtDesc(postId,
        pageable);
    return reviews.map(this::convertToResponseDto);
  }

  /**
   * 특정 게시글의 리뷰 통계 조회
   */
  public ReviewStatsDto getReviewStats(Integer postId) {
    BigDecimal averageRating = reviewRepository.findAverageRatingByPostId(postId)
        .orElse(BigDecimal.ZERO);

    Long totalReviews = reviewRepository.countByPostPostId(postId);

    Long ratingCount1 = reviewRepository.countByPostIdAndRatingRange(postId, BigDecimal.valueOf(0),
        BigDecimal.valueOf(1));
    Long ratingCount2 = reviewRepository.countByPostIdAndRatingRange(postId, BigDecimal.valueOf(1),
        BigDecimal.valueOf(2));
    Long ratingCount3 = reviewRepository.countByPostIdAndRatingRange(postId, BigDecimal.valueOf(2),
        BigDecimal.valueOf(3));
    Long ratingCount4 = reviewRepository.countByPostIdAndRatingRange(postId, BigDecimal.valueOf(3),
        BigDecimal.valueOf(4));
    Long ratingCount5 = reviewRepository.countByPostIdAndRatingRange(postId, BigDecimal.valueOf(4),
        BigDecimal.valueOf(5));

    return ReviewStatsDto.builder()
        .averageRating(averageRating.setScale(2, RoundingMode.HALF_UP))
        .totalReviews(totalReviews)
        .ratingCount1(ratingCount1)
        .ratingCount2(ratingCount2)
        .ratingCount3(ratingCount3)
        .ratingCount4(ratingCount4)
        .ratingCount5(ratingCount5)
        .build();
  }

  /**
   * 리뷰 수정
   */
  @Transactional
  public ReviewResponseDto updateReview(Integer userId, Integer reviewId,
      ReviewRequestDto requestDto) {
    ReviewEntity review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

    if (!review.getUser().getUserId().equals(userId)) {
      throw new IllegalArgumentException("본인이 작성한 리뷰만 수정할 수 있습니다.");
    }

    if (requestDto.getRating().compareTo(BigDecimal.ZERO) < 0 ||
        requestDto.getRating().compareTo(BigDecimal.valueOf(5)) > 0) {
      throw new IllegalArgumentException("평점은 0.0 ~ 5.0 사이여야 합니다.");
    }

    review.setRating(requestDto.getRating());
    review.setComment(requestDto.getComment());

    ReviewEntity updatedReview = reviewRepository.save(review);

    return convertToResponseDto(updatedReview);
  }

  /**
   * 리뷰 삭제
   */
  @Transactional
  public void deleteReview(Integer userId, Integer reviewId) {
    ReviewEntity review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

    if (!review.getUser().getUserId().equals(userId)) {
      throw new IllegalArgumentException("본인이 작성한 리뷰만 삭제할 수 있습니다.");
    }

    reviewRepository.delete(review);
  }

  /**
   * 사용자의 리뷰 작성 여부 확인
   */
  public boolean hasUserReviewedPost(Integer userId, Integer postId) {
    return reviewRepository.existsByPostPostIdAndUserUserId(postId, userId);
  }

  /**
   * 사용자의 특정 게시글 리뷰 조회
   */
  public ReviewResponseDto getUserReviewForPost(Integer userId, Integer postId) {
    // 수정된 부분
    ReviewEntity review = reviewRepository.findByPostPostIdAndUserUserId(postId, userId)
        .orElseThrow(() -> new IllegalArgumentException("작성한 리뷰가 없습니다."));

    return convertToResponseDto(review);
  }

  /**
   * 리뷰 신고
   */
  @Transactional
  public void reportReview(Integer reviewId) {
    ReviewEntity review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

    review.setReportCount(review.getReportCount() + 1);
    reviewRepository.save(review);
  }

  /**
   * Entity를 ResponseDto로 변환
   */
  private ReviewResponseDto convertToResponseDto(ReviewEntity review) {
    return ReviewResponseDto.builder()
        .reviewId(review.getId())
        .postId(review.getPost().getPostId())
        .userId(review.getUser().getUserId())
        .nickname(review.getUser().getNickname())
        .rating(review.getRating())
        .comment(review.getComment())
        .reportCount(review.getReportCount())
        .createdAt(review.getCreatedAt())
        .updatedAt(review.getUpdatedAt())
        .build();
  }
}