package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.Post;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

  @Query(value = """
          SELECT p.post_id AS postId,
                 p.title AS title,
                 p.food_name AS foodName,
                 p.rcp_steps_img AS rcpImgUrl,
                 p.view_count AS viewCount,
                 p.like_count AS likeCount,
                 p.created_at AS createdAt,
                 SUM(CASE WHEN i.name IN :subIngredients THEN 1 ELSE 0 END) AS subScore
          FROM post p
          JOIN recipe_ingredient ri ON p.post_id = ri.post_id
          JOIN ingredient i ON ri.ing_id = i.ing_id
          WHERE i.name IN :mainIngredients OR i.sort IN (
              SELECT sort FROM ingredient WHERE name IN :mainIngredients
          )
          GROUP BY p.post_id
          HAVING COUNT(DISTINCT CASE WHEN i.name IN :mainIngredients THEN i.name END) = :mainCount
          ORDER BY subScore DESC,
                   CASE WHEN :sort = 'latest' THEN p.created_at END DESC,
                   CASE WHEN :sort = 'views' THEN p.view_count END DESC,
                   CASE WHEN :sort = 'likes' THEN p.like_count END DESC
          LIMIT :limit
      """, nativeQuery = true)
  List<Object[]> searchByIngredients(
      @Param("mainIngredients") List<String> mainIngredients,
      @Param("subIngredients") List<String> subIngredients,
      @Param("mainCount") int mainCount,
      @Param("sort") String sort,
      @Param("limit") int limit
  );
}
