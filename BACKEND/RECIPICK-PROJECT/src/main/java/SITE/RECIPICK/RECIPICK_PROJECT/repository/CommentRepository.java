package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * COMMENT 테이블 접근 레포지토리
 */
public interface CommentRepository extends JpaRepository<Comment, Integer> {

  /**
   * 특정 유저가 작성한 댓글 개수 조회 - MyPageService에서 "활동 수(activityCount)" 계산할 때 사용
   */
  long countByAuthorId(Integer userId);
}
