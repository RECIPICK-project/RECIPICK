package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.UserEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * USER 테이블 접근 레포지토리
 */
public interface UserRepository extends JpaRepository<UserEntity, Integer> {

  long countByActiveTrue();

  List<UserEntity> findAllByOrderByIdDesc(Pageable pageable);
}