package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<PostEntity, Long> {

  // ========== 마이페이지: 내가 좋아요한 레시피 ==========
  // PostLikeEntity에 필드명이 postEntity/userEntity 라는 전제
  @Query("""
      select pl.postEntity
        from PostLikeEntity pl
       where pl.userEntity.id = :userId
       order by pl.createdAt desc
      """)
  List<PostEntity> findLikedPosts(@Param("userId") Integer userId, Pageable pageable);

  // ========== 마이페이지: 내 레시피 목록 (정식/임시) ==========
  // PostEntity에 필드명이 userEntity 라는 전제 (유도 메서드는 필드 경로를 그대로 써야 함)
  List<PostEntity> findByUserEntity_IdAndRcpIsOfficialTrueOrderByCreatedAtDesc(
      Integer userId, Pageable pageable
  );

  // [관리자] 전체 임시글 (userId 없음)
  List<PostEntity> findByRcpIsOfficialFalseOrderByCreatedAtDesc(Pageable pageable);

  List<PostEntity> findByUserEntity_IdAndRcpIsOfficialFalseOrderByCreatedAtDesc(
      Integer userId, Pageable pageable
  );

  // ========== 마이페이지: 집계 ==========
  @Query("""
      select count(p)
        from PostEntity p
       where p.userEntity.id = :userId
         and p.rcpIsOfficial = true
      """)
  long countPublishedByAuthor(@Param("userId") Integer userId);

  @Query("""
      select coalesce(sum(p.likeCount), 0)
        from PostEntity p
       where p.userEntity.id = :userId
         and p.rcpIsOfficial = true
      """)
  long sumLikesOnUsersPublished(@Param("userId") Integer userId);

  // ========== 관리자 대시보드용 집계 ==========
  long countByRcpIsOfficialTrue();

  long countByReportCountGreaterThan(int min);

  // ========== 관리자: 신고 많은 레시피 목록 ==========
  List<PostEntity> findByReportCountGreaterThanOrderByReportCountDesc(int min, Pageable pageable);

  // ========== 필요 시: id 집합으로 조회 ==========
  List<PostEntity> findByPostIdIn(Collection<Long> ids);
}
