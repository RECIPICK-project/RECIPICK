package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.SearchPostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.service.SearchService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

  private final SearchService searchService;

  @GetMapping("/search_page")
  public String search_page() {
    return "search";
  }

  @GetMapping("/search")
  public ResponseEntity<Map<String, Object>> searchRecipes(
      @RequestParam List<String> main,
      @RequestParam(required = false) List<String> sub,
      @RequestParam(defaultValue = "latest") String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    try {
      List<String> mainIngredients = main;
      List<String> subIngredients = sub;

      Pageable pageable = PageRequest.of(page, size);
      Map<String, Object> searchResult =
          searchService.searchRecipes(mainIngredients, subIngredients, sort, pageable);

      @SuppressWarnings("unchecked")
      List<SearchPostDto> recipes = (List<SearchPostDto>) searchResult.get("recipes");
      Integer totalCount = (Integer) searchResult.get("totalCount");

      return ResponseEntity.ok(
          Map.of(
              "success",
              true,
              "message",
              "검색이 완료되었습니다.",
              "mainIngredients",
              mainIngredients,
              "subIngredients",
              subIngredients != null ? subIngredients : List.of(),
              "recipes",
              recipes,
              "recipeCount",
              totalCount));

    } catch (Exception e) {
      log.error("통합 검색 중 오류 발생", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              Map.of(
                  "success",
                  false,
                  "message",
                  "검색 중 오류가 발생했습니다.",
                  "recipes",
                  List.of()));
    }
  }

  @GetMapping("/search/by-title")
  public ResponseEntity<Map<String, Object>> searchRecipesByTitle(
      @RequestParam String title,
      @RequestParam(defaultValue = "latest") String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    try {
      Pageable pageable = PageRequest.of(page, size);
      Map<String, Object> searchResult =
          searchService.searchRecipesByTitle(title, sort, pageable);

      @SuppressWarnings("unchecked")
      List<SearchPostDto> recipes = (List<SearchPostDto>) searchResult.get("recipes");
      Integer totalCount = (Integer) searchResult.get("totalCount");

      return ResponseEntity.ok(
          Map.of(
              "success",
              true,
              "message",
              "제목으로 검색 완료",
              "recipes",
              recipes,
              "totalCount",
              totalCount));
    } catch (Exception e) {
      log.error("제목으로 검색 중 오류 발생", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              Map.of(
                  "success",
                  false,
                  "message",
                  "제목 검색 중 오류가 발생했습니다.",
                  "recipes",
                  List.of(),
                  "totalCount",
                  0));
    }
  }

  // 카테고리로 레시피 검색 API
  @GetMapping("/search/by-category")
  public ResponseEntity<Map<String, Object>> searchRecipesByCategory(
      @RequestParam String category,
      @RequestParam(defaultValue = "latest") String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    try {
      Pageable pageable = PageRequest.of(page, size);
      Map<String, Object> searchResult =
          searchService.searchRecipesByCategory(category, sort, pageable);

      @SuppressWarnings("unchecked")
      List<SearchPostDto> recipes = (List<SearchPostDto>) searchResult.get("recipes");
      Integer totalCount = (Integer) searchResult.get("totalCount");

      return ResponseEntity.ok(
          Map.of(
              "success",
              true,
              "message",
              "카테고리로 검색 완료",
              "recipes",
              recipes,
              "totalCount",
              totalCount));
    } catch (Exception e) {
      log.error("카테고리로 검색 중 오류 발생", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              Map.of(
                  "success",
                  false,
                  "message",
                  "카테고리 검색 중 오류가 발생했습니다.",
                  "recipes",
                  List.of(),
                  "totalCount",
                  0));
    }
  }


  @GetMapping("/search/popular")
  public ResponseEntity<Map<String, Object>> getPopularRecipes(
      @RequestParam(defaultValue = "latest") String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    try {
      Pageable pageable = PageRequest.of(page, size);
      Map<String, Object> searchResult = searchService.getPopularRecipes(sort, pageable);

      @SuppressWarnings("unchecked")
      List<SearchPostDto> recipes = (List<SearchPostDto>) searchResult.get("recipes");
      Integer totalCount = (Integer) searchResult.get("totalCount");

      return ResponseEntity.ok(
          Map.of(
              "success",
              true,
              "message",
              "인기 레시피 조회 완료",
              "recipes",
              recipes,
              "totalCount",
              totalCount));
    } catch (Exception e) {
      log.error("인기 레시피 조회 중 오류 발생", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              Map.of(
                  "success",
                  false,
                  "message",
                  "인기 레시피 조회 중 오류가 발생했습니다.",
                  "recipes",
                  List.of(),
                  "totalCount",
                  0));
    }
  }

  @GetMapping("/search_home")
  public String search_home() {
    return "search_home";
  }

  @GetMapping("/ingredients")
  public ResponseEntity<List<String>> searchIngredients(
      @RequestParam String keyword, @RequestParam(defaultValue = "10") int limit) {
    try {
      List<String> ingredients = searchService.searchIngredients(keyword, limit);
      return ResponseEntity.ok(ingredients);
    } catch (Exception e) {
      log.error("재료 자동완성 검색 중 오류 발생", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
    }
  }
}
