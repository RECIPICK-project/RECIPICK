package SITE.RECIPICK.RECIPICK_PROJECT.service;

public class MyLikeService {

  public interface LikeService {

    boolean isLiked(Integer userId, Integer postId);

    void like(Integer userId, Integer postId);

    void unlike(Integer userId, Integer postId);
  }
}
