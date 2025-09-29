package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.UserEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, Integer> {

  Optional<UserEntity> findByEmail(String email);

  Optional<UserEntity> findByNickname(String nickname);

  boolean existsByEmail(String email);

  boolean existsByNickname(String nickname);

  // 활성 사용자 수
  long countByActiveTrue();

  // userId 기준 최신순
  List<UserEntity> findAllByOrderByUserIdDesc(Pageable pageable);

  // 가입일 기준 최신순 (JPQL)
  @Query("""
      select u
      from UserEntity u
      order by u.createdAt desc
      """)
  List<UserEntity> findTopNByOrderByCreatedAtDesc(Pageable pageable);

  // default 메서드: top N 개
  default List<UserEntity> findTopNByOrderByCreatedAtDesc(int top) {
    return findTopNByOrderByCreatedAtDesc(PageRequest.of(0, Math.max(1, top)));
  }

  // 이메일 → userId만 projection으로 가져오기
  @Query("select u.userId from UserEntity u where u.email = :email")
  Optional<Integer> findIdByEmail(@Param("email") String email);

  // =========================
  // DAU 집계용 핵심 메서드
  // - 지정한 기간 [start, end] 사이에 활동한 사용자 수 (DISTINCT userId)
  // - 시간대 변환(KST↔UTC)은 서비스에서 처리해서 넘겨줄 것
  // =========================
  @Query("""
      select count(distinct u.userId)
      from UserEntity u
      where u.latestAt between :start and :end
      """)
  long countDistinctActiveBetween(@Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);
}
