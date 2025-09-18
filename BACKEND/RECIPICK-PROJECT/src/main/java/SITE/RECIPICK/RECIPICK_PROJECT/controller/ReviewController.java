package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.ReviewDto;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.UserEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.service.ReviewService;
import SITE.RECIPICK.RECIPICK_PROJECT.service.UserService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
@Slf4j
public class ReviewController {

  private final ReviewService reviewService;
  private final UserService userService;

  /**
   * 리뷰 작성
   */
  @PostMapping
  public ResponseEntity<?> createReview(@Valid @RequestBody ReviewDto reviewDto) {
    log.info("=== 리뷰 작성 요청 시작 ===");
    log.info("Request Body: postId={}, reviewRating={}, comment length={}",
        reviewDto.getPostId(), reviewDto.getReviewRating(),
        reviewDto.getComment() != null ? reviewDto.getComment().length() : 0);

    try {
      // 인증 정보 디버깅
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      log.info("Authentication: name={}, principal={}, authenticated={}",
          auth != null ? auth.getName() : "null",
          auth != null ? auth.getPrincipal() : "null",
          auth != null && auth.isAuthenticated());

      Integer userId = getCurrentUserId();
      log.info("Retrieved userId: {}", userId);

      if (userId == null) {
        log.warn("사용자 인증 실패 - userId가 null");
        return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다."));
      }

      // 입력값 검증 로그
      if (reviewDto.getPostId() == null) {
        log.error("postId가 null입니다");
        return ResponseEntity.badRequest().body(Map.of("error", "postId가 필요합니다."));
      }

      if (reviewDto.getReviewRating() == null) {
        log.error("rating이 null입니다");
        return ResponseEntity.badRequest().body(Map.of("error", "rating이 필요합니다."));
      }

      if (reviewDto.getComment() == null || reviewDto.getComment().trim().isEmpty()) {
        log.error("comment가 비어있습니다");
        return ResponseEntity.badRequest().body(Map.of("error", "댓글 내용이 필요합니다."));
      }

      log.info("서비스 호출 전 - userId: {}, postId: {}", userId, reviewDto.getPostId());
      ReviewDto response = reviewService.createReview(userId, reviewDto);
      log.info("리뷰 작성 성공 - reviewId: {}", response.getReviewId());

      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      log.warn("리뷰 작성 실패 (검증 오류): {}", e.getMessage());
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("리뷰 작성 중 예상치 못한 오류", e);
      return ResponseEntity.internalServerError().body(Map.of("error", "서버 오류가 발생했습니다."));
    } finally {
      log.info("=== 리뷰 작성 요청 종료 ===");
    }
  }

  /**
   * 특정 게시글의 리뷰 목록 조회
   */
  @GetMapping("/post/{postId}")
  public ResponseEntity<Page<ReviewDto>> getReviewsByPostId(
      @PathVariable Integer postId,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

    try {
      log.info("리뷰 목록 조회 - postId: {}", postId);
      Page<ReviewDto> reviews = reviewService.getReviewsByPostId(postId, pageable);
      log.info("리뷰 목록 조회 성공 - 총 {}개", reviews.getTotalElements());
      return ResponseEntity.ok(reviews);
    } catch (Exception e) {
      log.error("리뷰 목록 조회 중 서버 오류", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * 특정 게시글의 리뷰 통계 조회
   */
  @GetMapping("/post/{postId}/stats")
  public ResponseEntity<ReviewDto> getReviewStats(@PathVariable Integer postId) {
    try {
      ReviewDto stats = reviewService.getReviewStats(postId);
      return ResponseEntity.ok(stats);
    } catch (Exception e) {
      log.error("리뷰 통계 조회 중 서버 오류", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * 리뷰 삭제 (userId 기반 권한 확인)
   */
  @DeleteMapping("/{reviewId}")
  public ResponseEntity<?> deleteReview(@PathVariable Integer reviewId) {
    try {
      Integer currentUserId = getCurrentUserId();
      if (currentUserId == null) {
        log.warn("리뷰 삭제 시도 - 인증되지 않은 사용자");
        return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다."));
      }

      log.info("리뷰 삭제 시도 - reviewId: {}, currentUserId: {}", reviewId, currentUserId);

      reviewService.deleteReview(currentUserId, reviewId);
      log.info("리뷰 삭제 성공 - reviewId: {}, userId: {}", reviewId, currentUserId);

      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      log.warn("리뷰 삭제 실패: {}", e.getMessage());
      if (e.getMessage().contains("본인이 작성한")) {
        return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
      }
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("리뷰 삭제 중 서버 오류", e);
      return ResponseEntity.internalServerError().body(Map.of("error", "서버 오류가 발생했습니다."));
    }
  }

  /**
   * 사용자의 리뷰 작성 여부 확인 (userId 기반)
   */
  @GetMapping("/post/{postId}/user-status")
  public ResponseEntity<?> hasUserReviewedPost(@PathVariable Integer postId) {
    try {
      Integer currentUserId = getCurrentUserId();
      if (currentUserId == null) {
        return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다."));
      }

      boolean hasReviewed = reviewService.hasUserReviewedPost(currentUserId, postId);
      return ResponseEntity.ok(hasReviewed);
    } catch (Exception e) {
      log.error("리뷰 작성 상태 확인 중 서버 오류", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * 사용자의 특정 게시글 리뷰 조회 (userId 기반)
   */
  @GetMapping("/post/{postId}/user-review")
  public ResponseEntity<?> getUserReviewForPost(@PathVariable Integer postId) {
    try {
      Integer currentUserId = getCurrentUserId();
      if (currentUserId == null) {
        return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다."));
      }

      ReviewDto review = reviewService.getUserReviewForPost(currentUserId, postId);
      return ResponseEntity.ok(review);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      log.error("사용자 리뷰 조회 중 서버 오류", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * 리뷰 신고
   */
  @PostMapping("/{reviewId}/report")
  public ResponseEntity<?> reportReview(@PathVariable Integer reviewId) {
    try {
      reviewService.reportReview(reviewId);
      return ResponseEntity.ok().build();
    } catch (IllegalArgumentException e) {
      log.warn("리뷰 신고 실패: {}", e.getMessage());
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("리뷰 신고 중 서버 오류", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * Spring Security에서 현재 인증된 사용자의 userId 가져오기 (강화된 버전)
   */
  private Integer getCurrentUserId() {
    try {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

      if (authentication == null) {
        log.warn("Authentication이 null입니다");
        return null;
      }

      if (!authentication.isAuthenticated()) {
        log.warn("사용자가 인증되지 않았습니다");
        return null;
      }

      if ("anonymousUser".equals(authentication.getPrincipal())) {
        log.warn("익명 사용자입니다");
        return null;
      }

      String email = authentication.getName();
      if (email == null || email.trim().isEmpty()) {
        log.warn("사용자 email이 비어있습니다");
        return null;
      }

      log.debug("인증된 사용자 email: {}", email);
      Optional<UserEntity> userOpt = userService.getUserByEmail(email);

      if (userOpt.isPresent()) {
        Integer userId = userOpt.get().getUserId();
        log.debug("조회된 userId: {}", userId);
        return userId;
      } else {
        log.warn("email로 사용자를 찾을 수 없습니다: {}", email);
        return null;
      }
    } catch (Exception e) {
      log.error("현재 사용자 ID 조회 중 오류", e);
      return null;
    }
  }
}