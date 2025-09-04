package SITE.RECIPICK.RECIPICK_PROJECT.service;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.SearchPostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.SearchRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchService {

  private final SearchRepository searchRepository;

  /**
   * 재료로 레시피 검색 (페이지네이션 수정)
   */
  @SuppressWarnings("unchecked")
  public Map<String, Object> searchRecipes(List<String> mainIngredients,
      List<String> subIngredients, String sort, Pageable pageable) {

    if (mainIngredients == null || mainIngredients.isEmpty()) {
      return Map.of("recipes", List.of(), "totalCount", 0);
    }

    if (subIngredients == null) {
      subIngredients = List.of();
    }

    // 정렬 조건 검증 및 변환
    sort = validateAndConvertSort(sort);

    int limit = pageable.getPageSize();
    int offset = (int) pageable.getOffset();

    // 1. 전체 레시피 수를 먼저 조회 (메인 재료만으로 카운트)
    int totalCount = searchRepository.countSearchByIngredients(mainIngredients);

    // 2. 페이지네이션된 레시피 목록 조회
    List<Object[]> results = searchRepository.searchByIngredients(
        mainIngredients, subIngredients, sort, limit, offset);

    List<SearchPostDto> searchPostDtos = results.stream().map(this::mapToPostDto)
        .collect(Collectors.toList());

    return Map.of("recipes", searchPostDtos, "totalCount", totalCount);
  }

  /**
   * 제목으로 레시피 검색 (페이지네이션)
   */
  public List<SearchPostDto> searchRecipesByTitle(String title, String sort, Pageable pageable) {
    if (title == null || title.trim().isEmpty()) {
      return List.of();
    }

    // 정렬 조건 검증 및 변환
    sort = validateAndConvertSort(sort);

    int limit = pageable.getPageSize();
    int offset = (int) pageable.getOffset();

    List<Object[]> results = searchRepository.searchByTitle(title, sort, limit, offset);
    return results.stream().map(this::mapToPostDto).collect(Collectors.toList());
  }

  /**
   * 인기/전체 레시피 조회 (페이지네이션)
   */
  public List<SearchPostDto> getPopularRecipes(String sort, Pageable pageable) {
    // 정렬 조건 검증 및 변환
    sort = validateAndConvertSort(sort);

    int limit = pageable.getPageSize();
    int offset = (int) pageable.getOffset();

    List<Object[]> results = searchRepository.findPopularRecipes(sort, limit, offset);
    return results.stream().map(this::mapToPostDto).collect(Collectors.toList());
  }

  /**
   * 재료 자동완성
   */
  public List<String> searchIngredients(String keyword, int limit) {
    if (keyword == null || keyword.trim().isEmpty()) {
      return List.of();
    }

    return searchRepository.findIngredientsByKeyword(keyword.trim(), limit);
  }

  /**
   * 정렬 조건 검증 및 변환
   */
  private String validateAndConvertSort(String sort) {
    if (sort == null) {
      return "latest";
    }

    // 프론트엔드에서 오는 정렬 조건을 백엔드 형식으로 변환
    switch (sort) {
      case "popular":
      case "likes":
        return "likes";
      case "rating":
      case "views":
        return "views";
      case "latest":
      default:
        return "latest";
    }
  }

  /**
   * Object[] 결과를 PostDto로 변환하는 헬퍼 메서드
   */
  private SearchPostDto mapToPostDto(Object[] row) {
    SearchPostDto dto = new SearchPostDto();
    dto.setPostId(((Number) row[0]).longValue());
    dto.setTitle((String) row[1]);
    dto.setFoodName((String) row[2]);

    // 썸네일 이미지 URL 매핑 - rcp_img_url 컬럼값
    String imageUrl = (String) row[3];
    dto.setRcpImgUrl(imageUrl); // PostDto의 rcpImgUrl 필드에 설정

    dto.setViewCount(((Number) row[4]).intValue());
    dto.setLikeCount(((Number) row[5]).intValue());

    // createdAt 타입 캐스팅 오류 수정
    if (row[6] instanceof java.sql.Timestamp) {
      dto.setCreatedAt(((java.sql.Timestamp) row[6]).toLocalDateTime());
    }

    // 디버깅용 로그
    System.out.println(
        "SearchPostDto mapping - postId: " + dto.getPostId() + ", rcpImgUrl: "
            + dto.getRcpImgUrl());

    return dto;
  }
}