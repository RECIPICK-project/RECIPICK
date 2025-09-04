package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostLikeEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLikeEntity, Integer> {

  // user.id 기준으로 정렬/페이징
  List<PostLikeEntity> findByUserEntity_IdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
}
