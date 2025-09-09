package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.UserTestEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTestRepository extends JpaRepository<UserTestEntity, Integer> {

  Optional<UserTestEntity> findByEmail(String email);

  Optional<UserTestEntity> findByNickname(String nickname);

  boolean existsByEmail(String email);

  boolean existsByNickname(String nickname);

}