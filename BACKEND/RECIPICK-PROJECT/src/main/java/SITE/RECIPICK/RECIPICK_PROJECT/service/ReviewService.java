package SITE.RECIPICK.RECIPICK_PROJECT.service;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.ReviewDto;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.ReviewResponseDto;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.ReviewEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.UserEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.PostRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.ReviewRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // For constructor injection
@Transactional
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final PostRepository postRepository;
  private final UserRepository userRepository;

  /**
   * Create a new review for a post
   */
  public ReviewResponseDto createReview(Integer postId, ReviewDto requestDto) {
    // Find the associated Post and User entities
    PostEntity post = postRepository.findById(postId)
        .orElseThrow(() -> new EntityNotFoundException("Post not found with id: " + postId));

    UserEntity user = userRepository.findById(requestDto.getUserId())
        .orElseThrow(
            () -> new EntityNotFoundException("User not found with id: " + requestDto.getUserId()));

    // Check if the user has already reviewed this post
    reviewRepository.findByUser_UserIdAndPost_PostId(user.getUserId(), postId).ifPresent(r -> {
      throw new IllegalStateException("User has already reviewed this post.");
    });

    // Create a new ReviewEntity from the DTO
    ReviewEntity newReview = ReviewEntity.builder()
        .post(post)
        .user(user)
        .rating(BigDecimal.valueOf(requestDto.getRating()))
        .comment(requestDto.getComment())
        .build();

    // Save the new review
    ReviewEntity savedReview = reviewRepository.save(newReview);

    // Return the response DTO
    return new ReviewResponseDto(savedReview);
  }

  /**
   * Get all reviews for a specific post
   */
  @Transactional(readOnly = true)
  public List<ReviewResponseDto> getReviewsByPostId(Integer postId) {
    List<ReviewEntity> reviews = reviewRepository.findByPost_PostIdOrderByCreatedAtDesc(postId);
    return reviews.stream()
        .map(ReviewResponseDto::new)
        .collect(Collectors.toList());
  }

  /**
   * Update an existing review
   */
  public ReviewResponseDto updateReview(Long reviewId, ReviewDto requestDto) {
    ReviewEntity review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new EntityNotFoundException("Review not found with id: " + reviewId));

    // Optional: Add logic here to verify if the user is authorized to update this review.
    // e.g., if(review.getUser().getUserId().equals(requestDto.getUserId())) { ... }

    review.setRating(BigDecimal.valueOf(requestDto.getRating()));
    review.setComment(requestDto.getComment());

    // The transaction will automatically commit the changes
    return new ReviewResponseDto(review);
  }

  /**
   * Delete a review
   */
  public void deleteReview(Long reviewId) {
    if (!reviewRepository.existsById(reviewId)) {
      throw new EntityNotFoundException("Review not found with id: " + reviewId);
    }

    // Optional: Add logic here to verify if the user is authorized to delete this review.

    reviewRepository.deleteById(reviewId);
  }
}