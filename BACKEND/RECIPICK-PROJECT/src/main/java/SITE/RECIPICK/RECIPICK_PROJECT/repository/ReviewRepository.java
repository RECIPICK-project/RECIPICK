package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.ReviewEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, Integer> {

  // 특정 게시글의 모든 리뷰 조회 (페이징)
  Page<ReviewEntity> findByPostPostIdOrderByCreatedAtDesc(Integer postId, Pageable pageable);

  // 특정 사용자가 특정 게시글에 작성한 리뷰 존재 여부 확인
  boolean existsByPostPostIdAndUserUserId(Integer postId, Integer userId);

  // 특정 사용자가 특정 게시글에 작성한 리뷰 조회 (수정된 부분)
  Optional<ReviewEntity> findByPostPostIdAndUserUserId(Integer postId, Integer userId);

  // 특정 게시글의 평균 평점 계산
  @Query("SELECT AVG(r.rating) FROM ReviewEntity r WHERE r.post.postId = :postId")
  Optional<BigDecimal> findAverageRatingByPostId(@Param("postId") Integer postId);

  // 특정 게시글의 총 리뷰 수
  Long countByPostPostId(Integer postId);

  Long countByUserUserId(Integer userId);

  // 특정 게시글의 평점별 리뷰 수 계산
  @Query("SELECT COUNT(r) FROM ReviewEntity r WHERE r.post.postId = :postId AND r.rating >= :minRating AND r.rating < :maxRating")
  Long countByPostIdAndRatingRange(@Param("postId") Integer postId,
      @Param("minRating") BigDecimal minRating,
      @Param("maxRating") BigDecimal maxRating);

  // 특정 사용자의 모든 리뷰 조회
  List<ReviewEntity> findByUserUserIdOrderByCreatedAtDesc(Integer userId);

  // 신고 횟수가 특정 값 이상인 리뷰 조회
  List<ReviewEntity> findByReportCountGreaterThanEqualOrderByReportCountDesc(int reportCount);
}