package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.Post;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


/**
 *
 */
@Repository
public interface SearchRepository extends JpaRepository<Post, Long> {

  @Query(value = """
          SELECT p.post_id AS postId,
                 p.title AS title,
                 p.food_name AS foodName,
                 p.rcp_steps_img AS rcpImgUrl,
                 p.view_count AS viewCount,
                 p.like_count AS likeCount,
                 p.created_at AS createdAt,
                 COALESCE(sub_matches.subScore, 0) AS subScore
          FROM post p
          JOIN recipe_ingredient ri ON p.post_id = ri.post_id
          JOIN ingredient i ON ri.ing_id = i.ing_id
          LEFT JOIN (
              SELECT ri2.post_id,
                     SUM(CASE WHEN i2.name IN :subIngredients THEN 1 ELSE 0 END) AS subScore
              FROM recipe_ingredient ri2
              JOIN ingredient i2 ON ri2.ing_id = i2.ing_id
              WHERE i2.name IN :subIngredients
              GROUP BY ri2.post_id
          ) sub_matches ON p.post_id = sub_matches.post_id
          WHERE (
              -- 1. 메인 재료 직접 매칭 (재료명 자체)
              i.name IN :mainIngredients
              OR 
              -- 2. 메인 재료가 카테고리명인 경우 (sort 컬럼 매칭)
              i.sort IN :mainIngredients
              OR
              -- 3. 메인 재료와 같은 sort를 가진 재료 매칭
              i.sort IN (
                  SELECT DISTINCT i_main.sort 
                  FROM ingredient i_main 
                  WHERE i_main.name IN :mainIngredients 
                  AND i_main.sort IS NOT NULL
              )
          )
          GROUP BY p.post_id, p.title, p.food_name, p.rcp_steps_img, 
                   p.view_count, p.like_count, p.created_at, sub_matches.subScore
          HAVING COUNT(DISTINCT 
              CASE 
                  WHEN i.name IN :mainIngredients THEN i.name 
                  WHEN i.sort IN :mainIngredients THEN i.sort
                  WHEN i.sort IN (
                      SELECT DISTINCT i_main.sort 
                      FROM ingredient i_main 
                      WHERE i_main.name IN :mainIngredients 
                      AND i_main.sort IS NOT NULL
                  ) THEN i.sort
              END
          ) >= 1
          ORDER BY 
              COUNT(CASE WHEN i.name IN :mainIngredients THEN 1 END) DESC,
              -- 1순위: 서브재료 매칭 점수 (높을수록 우선)
              COALESCE(sub_matches.subScore, 0) DESC,
              -- 2순위: 메인재료 직접 매칭 우선 (재료명 직접 > 카테고리명 직접 > 같은 카테고리)
              COUNT(CASE WHEN i.sort IN :mainIngredients THEN 1 END) DESC,
              -- 3순위: 정렬 조건
              CASE WHEN :sort = 'latest' THEN p.created_at END DESC,
              CASE WHEN :sort = 'views' THEN p.view_count END DESC,
              CASE WHEN :sort = 'likes' THEN p.like_count END DESC,
              -- 4순위: 기본 정렬 (최신순)
              p.created_at DESC
          LIMIT :limit
      """, nativeQuery = true)
  List<Object[]> searchByIngredients(
      @Param("mainIngredients") List<String> mainIngredients,
      @Param("subIngredients") List<String> subIngredients,
      @Param("mainCount") int mainCount,
      @Param("sort") String sort,
      @Param("limit") int limit
  );

  // 재료 자동완성을 위한 메서드 추가
  @Query(value = """
          SELECT DISTINCT i.name
          FROM ingredient i
          WHERE i.name LIKE CONCAT('%', :keyword, '%')
          ORDER BY 
              CASE WHEN i.name LIKE CONCAT(:keyword, '%') THEN 1 ELSE 2 END,
              LENGTH(i.name),
              i.name
          LIMIT :limit
      """, nativeQuery = true)
  List<String> findIngredientsByKeyword(
      @Param("keyword") String keyword,
      @Param("limit") int limit
  );
}