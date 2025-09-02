package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.Post;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * POST 테이블 레포지토리 - 기본 CRUD 외에 마이페이지 집계용 커스텀 쿼리 제공
 * <p>
 * 주의: - 엔티티 필드명이 user / rcpIsOfficial / likeCount 이므로 JPQL도 동일하게 사용 - user_id는 USER의 PK 타입에 맞춰
 * Integer로 받음
 */
public interface PostRepository extends JpaRepository<Post, Long> {

  /**
   * 특정 유저가 작성한 "정식 레시피" 개수 rcp_is_official = true 인 게시글만 카운트 - MyPageService.myRecipeCount 용
   */
  @Query("select count(p) from Post p where p.user.id = :uid and p.rcpIsOfficial = true")
  long countPublishedByAuthor(@Param("uid") Integer userId);

  /**
   * 특정 유저의 "정식 레시피"들이 받은 좋아요 총합 - null 방지를 위해 coalesce 처리 - MyPageService.totalLikesOnMyPosts 용
   */
  @Query("select coalesce(sum(p.likeCount), 0) from Post p where p.user.id = :uid and p.rcpIsOfficial = true")
  long sumLikesOnUsersPublished(@Param("uid") Integer userId);

  @Query("""
      select p
        from PostLike l
        join l.post p
       where l.user.id = :uid
       order by l.createdAt desc
      """)
  List<Post> findLikedPosts(@Param("uid") Integer userId, Pageable pageable);

  // 정식 레시피(official=true)만, 내 것만, 최신순 페이징
  List<Post> findByUserIdAndRcpIsOfficialTrueOrderByCreatedAtDesc(Integer userId,
      Pageable pageable);

  // 임시 레시피(official=false)만, 내 것만, 최신순 페이징
  List<Post> findByUserIdAndRcpIsOfficialFalseOrderByCreatedAtDesc(Integer userId,
      Pageable pageable);

}
