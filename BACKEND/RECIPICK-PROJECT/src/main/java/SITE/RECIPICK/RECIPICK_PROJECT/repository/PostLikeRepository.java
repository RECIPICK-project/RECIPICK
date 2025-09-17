package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostLikeEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLikeEntity, Integer> {

  // user.id 기준으로 정렬/페이징
  List<PostLikeEntity> findByUserEntity_userIdOrderByCreatedAtDesc(
      Integer userId, Pageable pageable);

  //  추가: 특정 유저가 특정 게시글을 좋아요 했는지
  boolean existsByUserEntity_userIdAndPostEntity_postId(Integer userId, Integer postId);

  //  추가: 특정 유저의 특정 게시글 좋아요 삭제
  void deleteByUserEntity_userIdAndPostEntity_postId(Integer userId, Integer postId);
}
