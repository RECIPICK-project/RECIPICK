package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.ReviewRequestDto;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.ReviewResponseDto;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.ReviewStatsDto;
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
import org.springframework.web.bind.annotation.PatchMapping;
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
  public ResponseEntity<ReviewResponseDto> createReview(
      @Valid @RequestBody ReviewRequestDto requestDto,
      HttpSession session) {

    Integer userId = getUserIdFromSession(session);
    ReviewResponseDto response = reviewService.createReview(userId, requestDto);

    return ResponseEntity.ok(response);
  }

  /**
   * 특정 게시글의 리뷰 목록 조회
   */
  @GetMapping("/post/{postId}")
  public ResponseEntity<Page<ReviewResponseDto>> getReviewsByPostId(
      @PathVariable Integer postId,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

    Page<ReviewResponseDto> reviews = reviewService.getReviewsByPostId(postId, pageable);

    return ResponseEntity.ok(reviews);
  }

  /**
   * 특정 게시글의 리뷰 통계 조회
   */
  @GetMapping("/post/{postId}/stats")
  public ResponseEntity<ReviewStatsDto> getReviewStats(@PathVariable Integer postId) {
    ReviewStatsDto stats = reviewService.getReviewStats(postId);

    return ResponseEntity.ok(stats);
  }

  /**
   * 리뷰 수정
   */
  @PatchMapping("/{reviewId}")
  public ResponseEntity<ReviewResponseDto> updateReview(
      @PathVariable Integer reviewId,
      @Valid @RequestBody ReviewRequestDto requestDto,
      HttpSession session) {

    Integer userId = getUserIdFromSession(session);
    ReviewResponseDto response = reviewService.updateReview(userId, reviewId, requestDto);

    return ResponseEntity.ok(response);
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
  public ResponseEntity<ReviewResponseDto> getUserReviewForPost(
      @PathVariable Integer postId,
      HttpSession session) {

    Integer userId = getUserIdFromSession(session);
    ReviewResponseDto review = reviewService.getUserReviewForPost(userId, postId);

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

  /**
   * 세션에서 사용자 ID 가져오기
   */
  private Integer getUserIdFromSession(HttpSession session) {
    Integer userId = (Integer) session.getAttribute("userId");
    if (userId == null) {
      throw new IllegalArgumentException("로그인이 필요합니다.");
    }
    return userId;
  }
}