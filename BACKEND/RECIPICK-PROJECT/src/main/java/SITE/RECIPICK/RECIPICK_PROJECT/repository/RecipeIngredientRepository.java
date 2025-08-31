package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.RecipeIngredient;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {

  @Query("select ri.ingredient.name from RecipeIngredient ri where ri.post.postId = :postId")
  Set<String> findIngredientNamesByPostId(Integer postId);
}
