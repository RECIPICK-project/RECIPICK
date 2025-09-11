package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity.CookingCategory;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<PostEntity, Integer> { // ← PK Integer

  // ========== 마이페이지: 내가 좋아요한 레시피 ==========
  // PostLikeEntity가 postEntity/userEntity 연관이라면 그대로 OK (모델이 다르면 수정 필요)
  @Query("""
        select pl.postEntity
        from PostLikeEntity pl
        where pl.userEntity = :userId
        order by pl.createdAt desc
      """)
  List<PostEntity> findLikedPosts(@Param("userId") Integer userId, Pageable pageable);

  // ========== 마이페이지: 내 레시피 목록 (정식/임시) ==========
  // userId(정수 FK) + rcpIsOfficial(0/1) 기준
  List<PostEntity> findByUserIdAndRcpIsOfficialOrderByCreatedAtDesc(
      Integer userId, Integer rcpIsOfficial, Pageable pageable);

  // ========== [관리자] 전체 임시글 ==========
  List<PostEntity> findByRcpIsOfficialOrderByCreatedAtDesc(Integer rcpIsOfficial,
      Pageable pageable);

  // ========== 집계 ==========
  @Query("""
      select count(p)
      from PostEntity p
      where p.userId = :userId
        and p.rcpIsOfficial = 1
      """)
  long countPublishedByAuthor(@Param("userId") Integer userId);

  @Query("""
      select coalesce(sum(p.likeCount), 0)
      from PostEntity p
      where p.userId = :userId
        and p.rcpIsOfficial = 1
      """)
  long sumLikesOnUsersPublished(@Param("userId") Integer userId);

  long countByRcpIsOfficial(Integer rcpIsOfficial); // 0/1

  long countByReportCountGreaterThanEqual(int min);

  // ========== 관리자 대시보드용 집계 ==========
  @Query("""
      select p.ckgCategory as category, count(p) as cnt
      from PostEntity p
      where p.createdAt between :from and :to
      group by p.ckgCategory
      order by count(p) desc
      """)
  List<CategoryAgg> countByCategoryBetween(
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to
  );

  // ========== 신고 많은 레시피 목록 ==========
  List<PostEntity> findByReportCountGreaterThanOrderByReportCountDesc(
      int min, Pageable pageable);

  // ========== id 집합으로 조회 ==========
  List<PostEntity> findByPostIdIn(Collection<Integer> ids); // @Id 필드명 기준
  // (findByIdIn은 @Id가 postId라서 모호. 위 메서드만 남기는 걸 추천)

  interface CategoryAgg {

    CookingCategory getCategory(); // enum 그대로 받아서 서비스/DTO에서 라벨로 변환

    long getCnt();
  }
}
