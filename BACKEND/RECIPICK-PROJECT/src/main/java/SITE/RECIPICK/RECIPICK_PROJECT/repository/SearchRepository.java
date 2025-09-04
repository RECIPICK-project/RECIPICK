package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.SearchPost;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchRepository extends JpaRepository<SearchPost, Long> {

  /**
   * 재료로 레시피 검색 (페이지네이션 수정)
   */
  @Query(value = """
      SELECT p.post_id AS postId,
             p.title AS title,
             p.food_name AS foodName,
             p.rcp_img_url AS rcpImgUrl,
             p.view_count AS viewCount,
             p.like_count AS likeCount,
             p.created_at AS createdAt,
             COALESCE(main_matches.mainScore, 0) AS mainScore,
             COALESCE(sub_matches.subScore, 0) AS subScore
      FROM post p
      LEFT JOIN (
          SELECT ri.post_id,
                 SUM(CASE WHEN i.name IN :mainIngredients THEN 1 ELSE 0 END) AS mainScore
          FROM recipe_ingredient ri
          JOIN ingredient i ON ri.ing_id = i.ing_id
          WHERE i.name IN :mainIngredients
          GROUP BY ri.post_id
      ) AS main_matches ON p.post_id = main_matches.post_id
      LEFT JOIN (
          SELECT ri2.post_id,
                 SUM(CASE WHEN i2.name IN :subIngredients THEN 1 ELSE 0 END) AS subScore
          FROM recipe_ingredient ri2
          JOIN ingredient i2 ON ri2.ing_id = i2.ing_id
          WHERE i2.name IN :subIngredients
          GROUP BY ri2.post_id
      ) AS sub_matches ON p.post_id = sub_matches.post_id
      WHERE main_matches.mainScore > 0 OR sub_matches.subScore > 0
      ORDER BY
          main_matches.mainScore DESC,   -- 1순위: 메인 재료 매칭 점수
          sub_matches.subScore DESC,     -- 2순위: 서브 재료 매칭 점수
          CASE WHEN :sort = 'views' THEN p.view_count END DESC,
          CASE WHEN :sort = 'likes' THEN p.like_count END DESC,
          CASE WHEN :sort = 'latest' THEN p.created_at END DESC,
          p.created_at DESC
      LIMIT :limit OFFSET :offset
      """, nativeQuery = true)
  List<Object[]> searchByIngredients(
      @Param("mainIngredients") List<String> mainIngredients,
      @Param("subIngredients") List<String> subIngredients,
      @Param("sort") String sort,
      @Param("limit") int limit,
      @Param("offset") int offset
  );

  /**
   * 재료로 레시피 개수 조회
   */
  @Query(value = """
      SELECT count(DISTINCT p.post_id) FROM post p
      JOIN recipe_ingredient ri ON p.post_id = ri.post_id
      JOIN ingredient i ON ri.ing_id = i.ing_id
      WHERE (
          i.name IN :mainIngredients
          OR i.sort IN :mainIngredients
          OR i.sort IN (
              SELECT DISTINCT i_main.sort
              FROM ingredient i_main
              WHERE i_main.name IN :mainIngredients
              AND i_main.sort IS NOT NULL
          )
      )
      """, nativeQuery = true)
  int countSearchByIngredients(@Param("mainIngredients") List<String> mainIngredients);

  /**
   * 제목으로 레시피 검색 (페이지네이션)
   */
  @Query(value = """
          SELECT p.post_id AS postId,
                 p.title AS title,
                 p.food_name AS foodName,
                 p.rcp_img_url AS rcpImgUrl,
                 p.view_count AS viewCount,
                 p.like_count AS likeCount,
                 p.created_at AS createdAt
          FROM post p
          WHERE p.title LIKE CONCAT('%', :title, '%')
          ORDER BY
              CASE WHEN :sort = 'views' THEN p.view_count END DESC,
              CASE WHEN :sort = 'likes' THEN p.like_count END DESC,
              CASE WHEN :sort = 'latest' THEN p.created_at END DESC,
              p.created_at DESC
          LIMIT :limit OFFSET :offset
      """, nativeQuery = true)
  List<Object[]> searchByTitle(
      @Param("title") String title,
      @Param("sort") String sort,
      @Param("limit") int limit,
      @Param("offset") int offset
  );

  /**
   * 인기/전체 레시피 조회 (페이지네이션 수정)
   */
  @Query(value = """
          SELECT p.post_id AS postId,
                 p.title AS title,
                 p.food_name AS foodName,
                 p.rcp_steps_img AS rcpImgUrl,
                 p.view_count AS viewCount,
                 p.like_count AS likeCount,
                 p.created_at AS createdAt
          FROM post p
          ORDER BY 
              CASE WHEN :sort = 'views' THEN p.view_count END DESC,
              CASE WHEN :sort = 'likes' THEN p.like_count END DESC,
              CASE WHEN :sort = 'latest' THEN p.created_at END DESC,
              p.created_at DESC
          LIMIT :limit OFFSET :offset
      """, nativeQuery = true)
  List<Object[]> findPopularRecipes(
      @Param("sort") String sort,
      @Param("limit") int limit,
      @Param("offset") int offset
  );

  // 재료 자동완성을 위한 메서드
  @Query(value = """
          SELECT DISTINCT i.name
          FROM ingredient i
          WHERE i.name LIKE CONCAT('%', :keyword, '%')
          ORDER BY 
              CASE WHEN i.name LIKE CONCAT(:keyword, '%') THEN 1 ELSE 2 END,
              i.name
          LIMIT :limit
      """, nativeQuery = true)
  List<String> findIngredientsByKeyword(
      @Param("keyword") String keyword,
      @Param("limit") int limit
  );
}