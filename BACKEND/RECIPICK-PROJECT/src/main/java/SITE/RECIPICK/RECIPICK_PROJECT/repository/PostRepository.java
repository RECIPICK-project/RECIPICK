package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * POST 테이블 접근 레포지토리
 */
public interface PostRepository extends JpaRepository<PostEntity, Integer> {

  // 공식 레시피 개수
  long countByRcpIsOfficial(int official);

  // 신고 카운트가 특정 값 이상인 레시피 수
  long countByReportCountGreaterThanEqual(int minReports);

  // 카테고리별 업로드 수 (기간 내)
  @Query("""
      select p.ckgCategory as category, count(p) as cnt
      from PostEntity p
      where p.createdAt between :from and :to
      group by p.ckgCategory
      """)
  List<CategoryCountAgg> countByCategoryBetween(LocalDateTime from, LocalDateTime to);

  // 내가 좋아요한 레시피 목록 (최신순)
  @Query("""
      select p
      from PostEntity p
        join PostLikeEntity l
          on l.postEntity = p
      where l.userEntity.userId = :userId
      order by l.createdAt desc, p.createdAt desc
      """)
  java.util.List<PostEntity> findLikedPosts(@Param("userId") Integer userId, Pageable pageable);

  // 내가 올린 정식 레시피 개수
  @Query("SELECT COUNT(p) FROM PostEntity p WHERE p.userId = :userId AND p.rcpIsOfficial = 1")
  long countPublishedByAuthor(@Param("userId") Integer userId);

  // 내가 올린 정식 레시피들의 총 좋아요 수
  @Query("SELECT COALESCE(SUM(p.likeCount), 0) FROM PostEntity p WHERE p.userId = :userId AND p.rcpIsOfficial = 1")
  long sumLikesOnUsersPublished(@Param("userId") Integer userId);
  

  // 특정 유저가 올린 레시피 중 정식/임시 구분하여 최신순 조회
  List<PostEntity> findByUserIdAndRcpIsOfficialOrderByCreatedAtDesc(
      Integer userId,
      Integer rcpIsOfficial,
      Pageable pageable
  );

  // 최근 임시 레시피
  List<PostEntity> findByRcpIsOfficialOrderByCreatedAtDesc(int isOfficial, Pageable pageable);

  // 신고 많은 레시피
  List<PostEntity> findByReportCountGreaterThanOrderByReportCountDesc(int min, Pageable pageable);

  // 개별 조회
  Optional<PostEntity> findByPostId(Integer postId);

  // Projection 인터페이스 (카테고리별 count)
  interface CategoryCountAgg {

    PostEntity.CookingCategory getCategory();

    long getCnt();
  }
}
