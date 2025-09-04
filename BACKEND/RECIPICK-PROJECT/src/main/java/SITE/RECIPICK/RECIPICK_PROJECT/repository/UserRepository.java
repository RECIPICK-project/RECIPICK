package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.UserEntity;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * USER 테이블 접근 레포지토리
 */
public interface UserRepository extends JpaRepository<UserEntity, Integer> {

  long countByActiveTrue();

  List<UserEntity> findAllByOrderByIdDesc(Pageable pageable);

  @Query("""
      select u
      from UserEntity u
      order by u.createdAt desc
      """)
  List<UserEntity> findTopNByOrderByCreatedAtDesc(Pageable pageable);

  default List<UserEntity> findTopNByOrderByCreatedAtDesc(int top) {
    return findTopNByOrderByCreatedAtDesc(PageRequest.of(0, Math.max(1, top)));
  }
}