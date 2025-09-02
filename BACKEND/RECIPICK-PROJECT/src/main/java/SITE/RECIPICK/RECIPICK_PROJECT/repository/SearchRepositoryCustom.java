package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.Post;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SearchRepositoryCustom {

  Page<Post> searchRecipes(List<String> mainIngredients,
      List<String> subIngredients,
      String sortType,
      Pageable pageable);
}
