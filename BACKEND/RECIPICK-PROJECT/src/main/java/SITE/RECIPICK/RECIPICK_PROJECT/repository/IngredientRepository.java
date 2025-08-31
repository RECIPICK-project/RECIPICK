package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.Ingredient;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, Integer> {

  // 자동완성: 이름 LIKE 검색
  List<Ingredient> findTop10ByNameContainingIgnoreCase(String keyword);
}
