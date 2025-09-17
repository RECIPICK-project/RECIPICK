package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.ReviewDto;
import SITE.RECIPICK.RECIPICK_PROJECT.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

  private final ReviewService reviewService;

  /**
   * 리뷰 작성
   */
  @PostMapping
  public ResponseEntity<ReviewDto> createReview(
      @Valid @RequestBody ReviewDto reviewDto,
      HttpSession session) {

    Integer userId = getUserIdFromSession(session);
    ReviewDto response = reviewService.createReview(userId, reviewDto);

    return ResponseEntity.ok(response);
  }

  /**
   * 특정 게시글의 리뷰 목록 조회
   */
  @GetMapping("/post/{postId}")
  public ResponseEntity<Page<ReviewDto>> getReviewsByPostId(
      @PathVariable Integer postId,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

    Page<ReviewDto> reviews = reviewService.getReviewsByPostId(postId, pageable);
    return ResponseEntity.ok(reviews);
  }

  /**
   * 특정 게시글의 리뷰 통계 조회
   */
  @GetMapping("/post/{postId}/stats")
  public ResponseEntity<ReviewDto> getReviewStats(@PathVariable Integer postId) {
    ReviewDto stats = reviewService.getReviewStats(postId);
    return ResponseEntity.ok(stats);
  }

  /**
   * 리뷰 삭제
   */
  @DeleteMapping("/{reviewId}")
  public ResponseEntity<Void> deleteReview(
      @PathVariable Integer reviewId,
      HttpSession session) {

    Integer userId = getUserIdFromSession(session);
    reviewService.deleteReview(userId, reviewId);

    return ResponseEntity.noContent().build();
  }

  /**
   * 사용자의 리뷰 작성 여부 확인
   */
  @GetMapping("/post/{postId}/user-status")
  public ResponseEntity<Boolean> hasUserReviewedPost(
      @PathVariable Integer postId,
      HttpSession session) {

    Integer userId = getUserIdFromSession(session);
    boolean hasReviewed = reviewService.hasUserReviewedPost(userId, postId);

    return ResponseEntity.ok(hasReviewed);
  }

  /**
   * 사용자의 특정 게시글 리뷰 조회
   */
  @GetMapping("/post/{postId}/user-review")
  public ResponseEntity<ReviewDto> getUserReviewForPost(
      @PathVariable Integer postId,
      HttpSession session) {

    Integer userId = getUserIdFromSession(session);
    ReviewDto review = reviewService.getUserReviewForPost(userId, postId);

    return ResponseEntity.ok(review);
  }

  /**
   * 리뷰 신고
   */
  @PostMapping("/{reviewId}/report")
  public ResponseEntity<Void> reportReview(@PathVariable Integer reviewId) {
    reviewService.reportReview(reviewId);
    return ResponseEntity.ok().build();
  }

  private Integer getUserIdFromSession(HttpSession session) {
    Integer userId = (Integer) session.getAttribute("userId");
    if (userId == null) {
      throw new IllegalArgumentException("로그인이 필요합니다.");
    }
    return userId;
  }
}