package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostLikeEntity;

public interface PostLikeRepository extends JpaRepository<PostLikeEntity, Integer> {

    // user.id 기준으로 정렬/페이징
    List<PostLikeEntity> findByUserEntity_userIdOrderByCreatedAtDesc(
            Integer userId, Pageable pageable);
}
