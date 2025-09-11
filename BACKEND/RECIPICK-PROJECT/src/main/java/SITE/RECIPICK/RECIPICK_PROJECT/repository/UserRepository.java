package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * USER 테이블 접근 레포지토리
 */
public interface UserRepository extends JpaRepository<UserEntity, Integer> {

  long countByActiveTrue();

  List<UserEntity> findAllByOrderByUserIdDesc(Pageable pageable);

  @Query("""
      select u
      from UserEntity u
      order by u.createdAt desc
      """)
  List<UserEntity> findTopNByOrderByCreatedAtDesc(Pageable pageable);

  default List<UserEntity> findTopNByOrderByCreatedAtDesc(int top) {
    return findTopNByOrderByCreatedAtDesc(PageRequest.of(0, Math.max(1, top)));
  }

  Optional<UserEntity> findByEmail(String email);

  // ★ 필요한 건 userId 숫자뿐이니 projection으로 가볍게
  @Query("select u.userId from UserEntity u where u.email = :email")
  Optional<Integer> findIdByEmail(@Param("email") String email);

  Optional<UserEntity> findByNickname(String nickname);

  boolean existsByEmail(String email);

  boolean existsByNickname(String nickname);
}