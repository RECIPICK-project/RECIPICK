package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.SearchPostEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SearchRepositoryCustom {

  Page<SearchPostEntity> searchRecipes(List<String> mainIngredients,
      List<String> subIngredients,
      String sortType,
      Pageable pageable);
}
