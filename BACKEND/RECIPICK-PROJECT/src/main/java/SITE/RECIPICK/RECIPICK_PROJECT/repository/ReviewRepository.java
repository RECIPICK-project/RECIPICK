package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.ReviewEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, Integer> {

  // Find all reviews for a given post, ordered by creation date
  List<ReviewEntity> findByPost_PostIdOrderByCreatedAtDesc(Integer postId);

  // Check if a review exists by a user for a specific post
  Optional<ReviewEntity> findByUser_UserIdAndPost_PostId(Integer userId, Integer postId);

  long countByUser_UserId(Integer userUserId);
}