package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDto;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.repository.query.Param;

@Mapper
public interface WeatherRecommendRepository {

  @Select("""
  SELECT post_id AS postId, title, food_name AS foodName, rcp_img_url AS rcpImgUrl,
         like_count AS likeCount, view_count AS viewCount
  FROM post
  WHERE rcp_img_url IS NOT NULL AND rcp_img_url <> ''
    AND (
      food_name REGEXP '냉면|냉국|냉모밀|콩국수|비빔냉면|막국수|물냉면|열무국수|냉우동|냉라면'
      OR title REGEXP '냉면|냉국|냉모밀|콩국수|비빔냉면|막국수|물냉면|열무국수|냉우동|냉라면'
    )
  ORDER BY RAND()
  LIMIT #{limit}
  """)
  List<PostDto> hotTop(@Param("limit") int limit);

  @Select("""
  SELECT post_id AS postId, title, food_name AS foodName, rcp_img_url AS rcpImgUrl,
         like_count AS likeCount, view_count AS viewCount
  FROM post
  WHERE rcp_img_url IS NOT NULL AND rcp_img_url <> ''
    AND (
      food_name REGEXP '탕|찌개|전골|국밥|라면|수프|카레|찜'
      OR title   REGEXP '탕|찌개|전골|국밥|라면|수프|카레|찜'
    )
  ORDER BY RAND()
  LIMIT #{limit}
  """)
  List<PostDto> coldTop(@Param("limit") int limit);

  @Select("""
  SELECT post_id AS postId, title, food_name AS foodName, rcp_img_url AS rcpImgUrl,
         like_count AS likeCount, view_count AS viewCount
  FROM post
  WHERE rcp_img_url IS NOT NULL AND rcp_img_url <> ''
    AND (
      food_name REGEXP '전|부침개|파전|빈대떡|칼국수|수제비'
      OR title   REGEXP '전|부침개|파전|빈대떡|칼국수|수제비'
    )
  ORDER BY RAND()
  LIMIT #{limit}
  """)
  List<PostDto> rainyTop(@Param("limit") int limit);

  @Select("""
  SELECT post_id AS postId, title, food_name AS foodName, rcp_img_url AS rcpImgUrl,
         like_count AS likeCount, view_count AS viewCount
  FROM post
  WHERE rcp_img_url IS NOT NULL AND rcp_img_url <> ''
    AND (
      food_name REGEXP '떡국|만두국|호빵|호떡|군고구마'
      OR title   REGEXP '떡국|만두국|호빵|호떡|군고구마'
    )
  ORDER BY RAND()
  LIMIT #{limit}
  """)
  List<PostDto> snowyTop(@Param("limit") int limit);
}