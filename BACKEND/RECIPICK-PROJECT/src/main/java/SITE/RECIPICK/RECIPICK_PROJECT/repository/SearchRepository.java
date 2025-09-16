package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchRepository extends JpaRepository<PostEntity, Long> {

  /**
   * 재료로 레시피 검색 (메인 재료 필수, 서브 재료 우선순위) - 메인 재료 카테고리 매칭을 post 테이블의 ckg_category로 판단
   */
  @Query(value = """
      WITH recipe_scores AS (
          SELECT 
              p.post_id,
              p.title,
              p.food_name,
              p.rcp_img_url,
              p.view_count,
              p.like_count,
              p.created_at,
              p.ckg_category,
              -- 메인 재료 직접 매칭 점수
              COALESCE(SUM(CASE WHEN i.name IN :mainIngredients THEN 1 ELSE 0 END), 0) AS main_direct_score,
              -- 메인 재료 카테고리 매칭 점수 (post의 ckg_category 기준)
              CASE WHEN p.ckg_category IN :mainIngredients THEN 0.5 ELSE 0 END AS main_category_score,
              -- 서브 재료 매칭 점수
              COALESCE(SUM(CASE WHEN i.name IN :subIngredients THEN 1 ELSE 0 END), 0) AS sub_score
          FROM post p
          LEFT JOIN recipe_ingredient ri ON p.post_id = ri.post_id
          LEFT JOIN ingredient i ON ri.ing_id = i.ing_id
          GROUP BY p.post_id, p.title, p.food_name, p.rcp_img_url, p.view_count, p.like_count, p.created_at, p.ckg_category
      )
      SELECT 
          post_id AS postId,
          title,
          food_name AS foodName,
          rcp_img_url AS rcpImgUrl,
          view_count AS viewCount,
          like_count AS likeCount,
          created_at AS createdAt
      FROM recipe_scores
      WHERE 
          -- 메인 재료가 직접 매칭되거나 카테고리로 매칭되어야 함 (필수 조건)
          (main_direct_score > 0 OR main_category_score > 0)
      ORDER BY
          -- defaultsort일 때만 메인/서브 우선순위 적용
          CASE WHEN :sort = 'defaultsort' THEN main_direct_score END DESC,
          CASE WHEN :sort = 'defaultsort' THEN sub_score END DESC,
          CASE WHEN :sort = 'defaultsort' THEN main_category_score END DESC,
          -- 사용자 선택 정렬 (우선순위 무시하고 순수 정렬)
          CASE WHEN :sort = 'views' THEN view_count END DESC,
          CASE WHEN :sort = 'likes' THEN like_count END DESC,
          CASE WHEN :sort = 'latest' THEN created_at END DESC,
          -- 최종 기본 정렬
          created_at DESC
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
   * 재료로 레시피 개수 조회 (메인 재료 기준) - 메인 재료 카테고리 매칭을 post 테이블의 ckg_category로 판단
   */
  @Query(value = """
      SELECT COUNT(DISTINCT p.post_id) 
      FROM post p
      LEFT JOIN recipe_ingredient ri ON p.post_id = ri.post_id
      LEFT JOIN ingredient i ON ri.ing_id = i.ing_id
      WHERE 
          -- 메인 재료가 직접 매칭되거나 카테고리로 매칭되어야 함
          (i.name IN :mainIngredients OR p.ckg_category IN :mainIngredients)
      """, nativeQuery = true)
  int countSearchByIngredients(@Param("mainIngredients") List<String> mainIngredients);

  /**
   * 제목으로 레시피 검색
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
   * 제목으로 레시피 개수 조회
   */
  @Query(value = """
          SELECT COUNT(p.post_id)
          FROM post p
          WHERE p.title LIKE CONCAT('%', :title, '%')
      """, nativeQuery = true)
  int countSearchByTitle(@Param("title") String title);

  /**
   * 인기/전체 레시피 조회
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

  /**
   * 전체 레시피 개수 조회
   */
  @Query(value = "SELECT COUNT(p.post_id) FROM post p", nativeQuery = true)
  int countAllRecipes();

  /**
   * 재료 자동완성
   */
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