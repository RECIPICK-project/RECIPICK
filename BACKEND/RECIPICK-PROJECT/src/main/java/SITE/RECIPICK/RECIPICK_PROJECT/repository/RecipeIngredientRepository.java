package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.RecipeIngredient;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.RecipeIngredientId;

@Repository
public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, RecipeIngredientId> {

    List<RecipeIngredient> findByPostId(Integer postId);

    void deleteByPostId(Integer postId);
}