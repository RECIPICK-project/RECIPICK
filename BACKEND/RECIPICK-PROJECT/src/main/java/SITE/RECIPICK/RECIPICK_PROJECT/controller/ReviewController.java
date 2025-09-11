package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.ReviewDto;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.ReviewResponseDto;
import SITE.RECIPICK.RECIPICK_PROJECT.service.ReviewService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class ReviewController {

  private final ReviewService reviewService;

  // 1. Create a review for a post
  @PostMapping("/{postId}/reviews")
  public ResponseEntity<ReviewResponseDto> createReview(
      @PathVariable Integer postId,
      @RequestBody ReviewDto requestDto) {
    ReviewResponseDto createdReview = reviewService.createReview(postId, requestDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdReview);
  }

  // 2. Get all reviews for a post
  @GetMapping("/{postId}/reviews")
  public ResponseEntity<List<ReviewResponseDto>> getReviewsByPost(@PathVariable Integer postId) {
    List<ReviewResponseDto> reviews = reviewService.getReviewsByPostId(postId);
    return ResponseEntity.ok(reviews);
  }

  // 3. Update a review
  @PutMapping("/reviews/{reviewId}")
  public ResponseEntity<ReviewResponseDto> updateReview(
      @PathVariable Integer reviewId,
      @RequestBody ReviewDto requestDto) {
    ReviewResponseDto updatedReview = reviewService.updateReview(reviewId, requestDto);
    return ResponseEntity.ok(updatedReview);
  }

  // 4. Delete a review
  @DeleteMapping("/reviews/{reviewId}")
  public ResponseEntity<Void> deleteReview(@PathVariable Integer reviewId) {
    reviewService.deleteReview(reviewId);
    return ResponseEntity.noContent().build();
  }
}