package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * REVIEW 테이블 접근 레포지토리 - 기본 CRUD 제공 (findById, save, delete 등) - 커스텀 메서드: 특정 유저가 작성한 리뷰(별점) 개수 카운트 →
 * MyPageService에서 "activityCount" 계산할 때 사용
 */
public interface ReviewRepository extends JpaRepository<Review, Integer> {

  /**
   * 작성자 ID 기준으로 리뷰 개수 조회
   */
  long countByAuthorId(Integer userId);
}
