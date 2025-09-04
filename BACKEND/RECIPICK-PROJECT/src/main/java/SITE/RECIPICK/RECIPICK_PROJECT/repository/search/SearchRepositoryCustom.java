package SITE.RECIPICK.RECIPICK_PROJECT.repository.search;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.search.SearchPost;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SearchRepositoryCustom {

  Page<SearchPost> searchRecipes(List<String> mainIngredients,
      List<String> subIngredients,
      String sortType,
      Pageable pageable);
}
