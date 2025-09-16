package SITE.RECIPICK.RECIPICK_PROJECT.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.SearchPostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.SearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final SearchRepository searchRepository;

    /** 재료로 레시피 검색 (메인 재료 필수, 서브 재료 우선순위) */
    public Map<String, Object> searchRecipes(
            List<String> mainIngredients,
            List<String> subIngredients,
            String sort,
            Pageable pageable) {

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

        try {
            // 1. 전체 레시피 수 조회 (메인 재료 기준만)
            int totalCount = searchRepository.countSearchByIngredients(mainIngredients);

            log.info(
                    "재료 검색 - 메인재료: {}, 서브재료: {}, 전체 개수: {}",
                    mainIngredients,
                    subIngredients,
                    totalCount);

            // 2. 페이지네이션된 레시피 목록 조회
            List<Object[]> results =
                    searchRepository.searchByIngredients(
                            mainIngredients, subIngredients, sort, limit, offset);

            List<SearchPostDto> searchPostDtos =
                    results.stream().map(this::mapToPostDto).collect(Collectors.toList());

            log.info("재료 검색 결과 - 반환된 레시피 수: {}", searchPostDtos.size());

            return Map.of("recipes", searchPostDtos, "totalCount", totalCount);

        } catch (Exception e) {
            log.error("재료 검색 중 오류 발생", e);
            return Map.of("recipes", List.of(), "totalCount", 0);
        }
    }

    /** 제목으로 레시피 검색 (전체 개수 포함) */
    public Map<String, Object> searchRecipesByTitle(String title, String sort, Pageable pageable) {
        if (title == null || title.trim().isEmpty()) {
            return Map.of("recipes", List.of(), "totalCount", 0);
        }

        // 정렬 조건 검증 및 변환
        sort = validateAndConvertSort(sort);

        int limit = pageable.getPageSize();
        int offset = (int) pageable.getOffset();

        try {
            // 1. 전체 검색 결과 개수 조회
            int totalCount = searchRepository.countSearchByTitle(title);

            log.info("제목 검색 - 검색어: '{}', 전체 개수: {}", title, totalCount);

            // 2. 페이지네이션된 레시피 목록 조회
            List<Object[]> results = searchRepository.searchByTitle(title, sort, limit, offset);

            List<SearchPostDto> searchPostDtos =
                    results.stream().map(this::mapToPostDto).collect(Collectors.toList());

            log.info("제목 검색 결과 - 반환된 레시피 수: {}", searchPostDtos.size());

            return Map.of("recipes", searchPostDtos, "totalCount", totalCount);

        } catch (Exception e) {
            log.error("제목 검색 중 오류 발생", e);
            return Map.of("recipes", List.of(), "totalCount", 0);
        }
    }

    /** 인기/전체 레시피 조회 (전체 개수 포함) */
    public Map<String, Object> getPopularRecipes(String sort, Pageable pageable) {
        // 정렬 조건 검증 및 변환
        sort = validateAndConvertSort(sort);

        int limit = pageable.getPageSize();
        int offset = (int) pageable.getOffset();

        try {
            // 1. 전체 레시피 개수 조회
            int totalCount = searchRepository.countAllRecipes();

            log.info("전체/인기 레시피 조회 - 전체 개수: {}", totalCount);

            // 2. 페이지네이션된 레시피 목록 조회
            List<Object[]> results = searchRepository.findPopularRecipes(sort, limit, offset);

            List<SearchPostDto> searchPostDtos =
                    results.stream().map(this::mapToPostDto).collect(Collectors.toList());

            log.info("전체/인기 레시피 결과 - 반환된 레시피 수: {}", searchPostDtos.size());

            return Map.of("recipes", searchPostDtos, "totalCount", totalCount);

        } catch (Exception e) {
            log.error("인기 레시피 조회 중 오류 발생", e);
            return Map.of("recipes", List.of(), "totalCount", 0);
        }
    }

    /** 재료 자동완성 */
    public List<String> searchIngredients(String keyword, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        return searchRepository.findIngredientsByKeyword(keyword.trim(), limit);
    }

    /** 정렬 조건 검증 및 변환 */
    private String validateAndConvertSort(String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            return "defaultsort";
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
                return "latest";
            case "defaultsort":
            case "":
                return "defaultsort";
            default:
                return "defaultsort";
        }
    }

    /** Object[] 결과를 PostDto로 변환하는 헬퍼 메서드 */
    private SearchPostDto mapToPostDto(Object[] row) {
        SearchPostDto dto = new SearchPostDto();
        dto.setPostId(((Number) row[0]).longValue());
        dto.setTitle((String) row[1]);
        dto.setFoodName((String) row[2]);

        // 썸네일 이미지 URL 매핑
        String imageUrl = (String) row[3];
        dto.setRcpImgUrl(imageUrl);

        dto.setViewCount(row[4] != null ? ((Number) row[4]).intValue() : 0);
        dto.setLikeCount(row[5] != null ? ((Number) row[5]).intValue() : 0);

        // createdAt 타입 캐스팅
        if (row[6] instanceof java.sql.Timestamp) {
            dto.setCreatedAt(((java.sql.Timestamp) row[6]).toLocalDateTime());
        }

        return dto;
    }
}
