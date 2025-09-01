package SITE.RECIPICK.RECIPICK_PROJECT.service;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.PostRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService {

  private final PostRepository postRepository;

  public List<PostDto> searchRecipes(List<String> mainIngredients, List<String> subIngredients,
      String sort, Pageable pageable) {

    // 빈 리스트 처리
    if (mainIngredients == null || mainIngredients.isEmpty()) {
      return List.of();
    }

    // null 체크 및 기본값 설정
    if (subIngredients == null) {
      subIngredients = List.of();
    }

    int mainCount = mainIngredients.size();
    int limit = pageable.getPageSize();

    // 정렬 조건 검증
    if (sort == null || (!sort.equals("latest") && !sort.equals("views") && !sort.equals(
        "likes"))) {
      sort = "latest"; // 기본값
    }

    List<Object[]> results = postRepository.searchByIngredients(
        mainIngredients, subIngredients, mainCount, sort, limit
    );

    return results.stream().map(row -> {
      PostDto dto = new PostDto();
      dto.setPostId(((Number) row[0]).longValue());
      dto.setTitle((String) row[1]);
      dto.setFoodName((String) row[2]);
      dto.setRcpImgUrl((String) row[3]);
      dto.setViewCount(((Number) row[4]).intValue());
      dto.setLikeCount(((Number) row[5]).intValue());
      dto.setCreatedAt(((java.sql.Timestamp) row[6]).toLocalDateTime());

      // subScore 안전하게 변환
      Object subScoreObj = row[7];
      if (subScoreObj instanceof Number) {
        dto.setSubScore(((Number) subScoreObj).intValue());
      } else {
        dto.setSubScore(0);
      }

      return dto;
    }).collect(Collectors.toList());
  }

  // 재료 자동완성 메서드 추가
  public List<String> searchIngredients(String keyword, int limit) {
    if (keyword == null || keyword.trim().isEmpty()) {
      return List.of();
    }

    return postRepository.findIngredientsByKeyword(keyword.trim(), limit);
  }
}