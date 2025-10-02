package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.RecipeIngredient;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.RecipeIngredientId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, RecipeIngredientId> {

    List<RecipeIngredient> findByPostId(Integer postId);

    void deleteByPostId(Integer postId);
}