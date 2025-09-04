package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.SearchPostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.service.OCRService;
import SITE.RECIPICK.RECIPICK_PROJECT.service.SearchService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

  private final SearchService searchService;
  private final OCRService ocrService;

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
      @RequestParam(defaultValue = "20") int size
  ) {
    try {
      List<String> mainIngredients = main;
      List<String> subIngredients = sub;

      Pageable pageable = PageRequest.of(page, size);
      Map<String, Object> searchResult = searchService.searchRecipes(mainIngredients,
          subIngredients, sort, pageable);

      List<SearchPostDto> recipes = (List<SearchPostDto>) searchResult.get("recipes");
      Integer totalCount = (Integer) searchResult.get("totalCount");

      return ResponseEntity.ok(Map.of(
          "success", true,
          "message", "검색이 완료되었습니다.",
          "mainIngredients", mainIngredients,
          "subIngredients", subIngredients != null ? subIngredients : List.of(),
          "recipes", recipes,
          "recipeCount", totalCount
      ));

    } catch (Exception e) {
      log.error("통합 검색 중 오류 발생", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
          "success", false,
          "message", "검색 중 오류가 발생했습니다.",
          "recipes", List.of()
      ));
    }
  }

  @GetMapping("/search/by-title")
  public ResponseEntity<Map<String, Object>> searchRecipesByTitle(
      @RequestParam String title,
      @RequestParam(defaultValue = "latest") String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    try {
      Pageable pageable = PageRequest.of(page, size);
      List<SearchPostDto> recipes = searchService.searchRecipesByTitle(title, sort, pageable);
      return ResponseEntity.ok(Map.of(
          "success", true,
          "message", "제목으로 검색 완료",
          "recipes", recipes
      ));
    } catch (Exception e) {
      log.error("제목으로 검색 중 오류 발생", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
          "success", false,
          "message", "제목 검색 중 오류가 발생했습니다.",
          "recipes", List.of()
      ));
    }
  }

  @GetMapping("/search/popular")
  public ResponseEntity<Map<String, Object>> getPopularRecipes(
      @RequestParam(defaultValue = "latest") String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    try {
      Pageable pageable = PageRequest.of(page, size);
      List<SearchPostDto> recipes = searchService.getPopularRecipes(sort, pageable);
      return ResponseEntity.ok(Map.of(
          "success", true,
          "message", "인기 레시피 조회 완료",
          "recipes", recipes
      ));
    } catch (Exception e) {
      log.error("인기 레시피 조회 중 오류 발생", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
          "success", false,
          "message", "인기 레시피 조회 중 오류가 발생했습니다.",
          "recipes", List.of()
      ));
    }
  }

  @GetMapping("/search_home")
  public String search_home() {
    return "search_home";
  }

  @GetMapping("/ingredients")
  public ResponseEntity<List<String>> searchIngredients(
      @RequestParam String keyword,
      @RequestParam(defaultValue = "10") int limit
  ) {
    try {
      List<String> ingredients = searchService.searchIngredients(keyword, limit);
      return ResponseEntity.ok(ingredients);
    } catch (Exception e) {
      log.error("재료 자동완성 검색 중 오류 발생", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
    }
  }

  @PostMapping("/ocr_search")
  public ResponseEntity<Map<String, Object>> ocrSearch(
      @RequestParam(required = false) MultipartFile image,
      @RequestParam(required = false) List<String> main,
      @RequestParam(required = false) List<String> sub,
      @RequestParam(defaultValue = "latest") String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) throws IOException, TesseractException {

    List<String> extractedIngredients = List.of();
    List<String> subIngredients = sub != null ? sub : List.of();
    List<String> mainIngredients = main != null ? main : List.of();

    try {
      if (image != null && !image.isEmpty()) {
        extractedIngredients = ocrService.extractIngredientsFromImage(image);
        mainIngredients.addAll(extractedIngredients);
      }

      if (mainIngredients.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of(
            "success", false,
            "message", "검색할 재료를 입력하거나 이미지를 업로드해주세요.",
            "extractedIngredients", extractedIngredients,
            "recipes", List.of()
        ));
      }

      Pageable pageable = PageRequest.of(page, size);
      Map<String, Object> searchResult = searchService.searchRecipes(mainIngredients,
          subIngredients,
          sort, pageable);

      // 경고를 해결하기 위해 명시적으로 캐스팅
      List<SearchPostDto> recipes = (List<SearchPostDto>) searchResult.get("recipes");
      Integer totalCount = (Integer) searchResult.get("totalCount");

      return ResponseEntity.ok(Map.of(
          "success", true,
          "message", "검색이 완료되었습니다.",
          "extractedIngredients", extractedIngredients,
          "mainIngredients", mainIngredients,
          "subIngredients", subIngredients,
          "recipes", recipes,
          "recipeCount", totalCount
      ));

    } catch (Exception e) {
      log.error("통합 검색 중 오류 발생", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
          "success", false,
          "message", "검색 중 오류가 발생했습니다.",
          "recipes", List.of()
      ));
    }
  }
}