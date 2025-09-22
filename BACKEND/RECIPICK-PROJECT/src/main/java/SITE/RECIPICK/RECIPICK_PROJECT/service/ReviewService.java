package SITE.RECIPICK.RECIPICK_PROJECT.service;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.ReviewDto;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.ReviewEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.UserEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.PostRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.ReviewRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.UserRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final PostRepository postRepository;
  private final UserRepository userRepository;

  /**
   * 리뷰 작성
   */
  public ReviewDto createReview(Integer userId, ReviewDto reviewDto) {
    log.info("리뷰 작성 요청 - userId: {}, postId: {}, rating: {}",
        userId, reviewDto.getPostId(), reviewDto.getReviewRating());

    // 기존 리뷰 존재 여부 확인
    boolean alreadyExists = reviewRepository.existsByPostPostIdAndUserUserId(
        reviewDto.getPostId(), userId);

    if (alreadyExists) {
      throw new IllegalArgumentException("이미 해당 게시글에 리뷰를 작성하셨습니다.");
    }

    validateReview(reviewDto);

    PostEntity post = postRepository.findById(reviewDto.getPostId())
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

    UserEntity user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

    ReviewEntity review = ReviewEntity.builder()
        .post(post)
        .user(user)
        .reviewRating(reviewDto.getReviewRating())
        .comment(reviewDto.getComment())
        .reportCount(0)
        .build();

    ReviewEntity savedReview = reviewRepository.save(review);
    log.info("리뷰 작성 완료 - reviewId: {}", savedReview.getReviewId());
    return ReviewDto.fromEntity(savedReview);
  }

  /**
   * 리뷰 목록 조회
   */
  @Transactional(readOnly = true)
  public Page<ReviewDto> getReviewsByPostId(Integer postId, Pageable pageable) {
    Page<ReviewEntity> reviewPage = reviewRepository.findByPostPostIdOrderByCreatedAtDesc(postId,
        pageable);
    return reviewPage.map(ReviewDto::fromEntity);
  }

  /**
   * 리뷰 통계 조회
   */
  @Transactional(readOnly = true)
  public ReviewDto getReviewStats(Integer postId) {
    BigDecimal avgRating = reviewRepository.findAverageRatingByPostId(postId)
        .orElse(BigDecimal.ZERO);

    Long totalReviews = reviewRepository.countByPostPostId(postId);

    // 각 평점별 개수 계산
    Long count1 = reviewRepository.countByPostIdAndRatingRange(postId,
        new BigDecimal("1.0"), new BigDecimal("2.0"));
    Long count2 = reviewRepository.countByPostIdAndRatingRange(postId,
        new BigDecimal("2.0"), new BigDecimal("3.0"));
    Long count3 = reviewRepository.countByPostIdAndRatingRange(postId,
        new BigDecimal("3.0"), new BigDecimal("4.0"));
    Long count4 = reviewRepository.countByPostIdAndRatingRange(postId,
        new BigDecimal("4.0"), new BigDecimal("5.0"));
    Long count5 = reviewRepository.countByPostIdAndRatingRange(postId,
        new BigDecimal("5.0"), new BigDecimal("6.0")); // 5.00 포함

    return ReviewDto.forStats(avgRating, totalReviews, count1, count2, count3, count4, count5);
  }

  /**
   * 리뷰 삭제
   */
  public void deleteReview(Integer userId, Integer reviewId) {
    ReviewEntity review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 리뷰입니다."));

    if (!review.getUser().getUserId().equals(userId)) {
      throw new IllegalArgumentException("본인이 작성한 리뷰만 삭제할 수 있습니다.");
    }

    reviewRepository.delete(review);
    log.info("리뷰 삭제 완료 - reviewId: {}, userId: {}", reviewId, userId);
  }

  /**
   * 사용자 리뷰 작성 여부 확인
   */
  @Transactional(readOnly = true)
  public boolean hasUserReviewedPost(Integer userId, Integer postId) {
    return reviewRepository.existsByPostPostIdAndUserUserId(postId, userId);
  }

  /**
   * 사용자의 특정 게시글 리뷰 조회
   */
  @Transactional(readOnly = true)
  public ReviewDto getUserReviewForPost(Integer userId, Integer postId) {
    ReviewEntity review = reviewRepository.findByPostPostIdAndUserUserId(postId, userId)
        .orElseThrow(() -> new IllegalArgumentException("해당 게시글에 작성한 리뷰가 없습니다."));

    return ReviewDto.fromEntity(review);
  }

  /**
   * 리뷰 신고
   */
  public void reportReview(Integer reviewId) {
    ReviewEntity review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 리뷰입니다."));

    review.setReportCount(review.getReportCount() + 1);
    reviewRepository.save(review);
  }

  /**
   * 리뷰 검증
   */
  private void validateReview(ReviewDto reviewDto) {
    if (reviewDto.getComment() == null || reviewDto.getComment().trim().isEmpty()) {
      throw new IllegalArgumentException("댓글 내용을 입력해주세요.");
    }

    if (reviewDto.getComment().length() > 255) {
      throw new IllegalArgumentException("댓글은 255자를 초과할 수 없습니다.");
    }

    if (reviewDto.getReviewRating() == null) {
      throw new IllegalArgumentException("평점을 입력해주세요.");
    }

    // 평점 범위 검증: 0.5 ~ 5.0 허용 (JavaScript에서 보내는 범위)
    BigDecimal minRating = new BigDecimal("0.5");
    BigDecimal maxRating = new BigDecimal("5.0");

    if (reviewDto.getReviewRating().compareTo(minRating) < 0 ||
        reviewDto.getReviewRating().compareTo(maxRating) > 0) {
      throw new IllegalArgumentException("평점은 0.5~5.0 사이의 값이어야 합니다.");
    }

    // 0.5 단위로만 허용 (JavaScript에서 0.5, 1.0, 1.5, ... 5.0 보냄)
    BigDecimal doubled = reviewDto.getReviewRating().multiply(new BigDecimal("2"));
    if (doubled.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
      throw new IllegalArgumentException("평점은 0.5 단위로만 입력 가능합니다.");
    }

    log.debug("리뷰 검증 완료 - rating: {}, comment length: {}",
        reviewDto.getReviewRating(), reviewDto.getComment().length());
  }
}