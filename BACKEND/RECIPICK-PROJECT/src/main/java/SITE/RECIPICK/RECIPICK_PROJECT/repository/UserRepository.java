package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * USER 테이블 접근 레포지토리
 */
public interface UserRepository extends JpaRepository<User, Integer> {

}
