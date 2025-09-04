// 기존 IngredientRepository에 OCR 기능을 위한 메서드들만 추가하세요

package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.Ingredient;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Integer> {

  // 1. keyword 포함하는 재료 이름 검색 (최대 10개)
  List<Ingredient> findTop10ByNameContainingIgnoreCase(String keyword);

  // 2. 정확히 일치하는 이름 검색
  List<Ingredient> findByName(String name);

}
