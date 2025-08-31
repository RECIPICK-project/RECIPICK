package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

  @Query("""
      select distinct p from Post p
      join p.recipeIngredients ri
      join ri.ingredient i
      where (:main is null or i.name = :main)
        and (:sub is null or i.name = :sub)
      """)
  Page<Post> search(@Param("main") String main,
      @Param("sub") String sub,
      Pageable pageable);
}
