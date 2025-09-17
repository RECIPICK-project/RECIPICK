package SITE.RECIPICK.RECIPICK_PROJECT.service;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostLikeEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.UserEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.PostLikeRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.PostRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.UserRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.service.MyLikeService.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

  private final PostLikeRepository likeRepo;
  private final PostRepository postRepo;
  private final UserRepository userRepo;

  @Override
  public boolean isLiked(Integer userId, Integer postId) {
    return likeRepo.existsByUserEntity_userIdAndPostEntity_postId(userId, postId);
  }

  @Transactional
  @Override
  public void like(Integer userId, Integer postId) {
    if (likeRepo.existsByUserEntity_userIdAndPostEntity_postId(userId, postId)) {
      return;
    }

    PostEntity post = postRepo.findById(postId)
        .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
    UserEntity user = userRepo.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

    PostLikeEntity like = new PostLikeEntity();
    like.setPostEntity(post);
    like.setUserEntity(user);
    likeRepo.save(like);

    int curr = post.getLikeCount() == null ? 0 : post.getLikeCount();
    post.setLikeCount(curr + 1);
    postRepo.save(post);
  }

  @Transactional
  @Override
  public void unlike(Integer userId, Integer postId) {
    if (!likeRepo.existsByUserEntity_userIdAndPostEntity_postId(userId, postId)) {
      return;
    }

    PostEntity post = postRepo.findById(postId)
        .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

    likeRepo.deleteByUserEntity_userIdAndPostEntity_postId(userId, postId);

    int curr = post.getLikeCount() == null ? 0 : post.getLikeCount();
    post.setLikeCount(Math.max(0, curr - 1));
    postRepo.save(post);
  }
}
