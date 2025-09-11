package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * PROFILE 테이블 접근 레포지토리 - PK = user_id
 */
public interface ProfileRepository extends JpaRepository<ProfileEntity, Integer> {

  /**
   * 닉네임 중복 여부 확인 (유니크 제약과 중복 방지용 사전 체크)
   */
  boolean existsByNickname(String nickname);
}
