package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.OCRResultDto;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDto;
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

  /**
   * 기존 재료 검색 기능 (텍스트 입력)
   */
  @GetMapping("/search")
  public List<PostDto> searchRecipes(
      @RequestParam List<String> main,
      @RequestParam(required = false) List<String> sub,
      @RequestParam(defaultValue = "latest") String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    if (sub == null) {
      sub = List.of();
    }
    Pageable pageable = PageRequest.of(page, size);
    return searchService.searchRecipes(main, sub, sort, pageable);
  }

  /**
   * 이미지에서 재료를 추출하여 레시피 검색
   */
  @PostMapping("/search-by-image")
  public ResponseEntity<?> searchRecipesByImage(
      @RequestParam("image") MultipartFile imageFile,
      @RequestParam(required = false) List<String> additionalIngredients,
      @RequestParam(defaultValue = "latest") String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    try {
      log.info("이미지 기반 레시피 검색 요청: {}", imageFile.getOriginalFilename());

      // 1. 이미지에서 재료 추출
      List<String> extractedIngredients = ocrService.extractIngredientsFromImage(imageFile);

      if (extractedIngredients.isEmpty()) {
        return ResponseEntity.ok(Map.of(
            "success", false,
            "message", "이미지에서 재료를 찾을 수 없습니다.",
            "extractedIngredients", extractedIngredients,
            "recipes", List.of()
        ));
      }

      // 2. 추가 재료가 있다면 합치기
      List<String> allIngredients = extractedIngredients;
      if (additionalIngredients != null && !additionalIngredients.isEmpty()) {
        allIngredients = new java.util.ArrayList<>(extractedIngredients);
        allIngredients.addAll(additionalIngredients);
      }

      // 3. 재료로 레시피 검색
      Pageable pageable = PageRequest.of(page, size);
      List<PostDto> recipes = searchService.searchRecipes(allIngredients, List.of(), sort,
          pageable);

      log.info("이미지 기반 검색 완료: {} 개 재료 추출, {} 개 레시피 검색됨",
          extractedIngredients.size(), recipes.size());

      return ResponseEntity.ok(Map.of(
          "success", true,
          "message", "검색이 완료되었습니다.",
          "extractedIngredients", extractedIngredients,
          "totalIngredients", allIngredients,
          "recipes", recipes,
          "recipeCount", recipes.size()
      ));

    } catch (IOException | TesseractException e) {
      log.error("이미지 처리 중 오류 발생", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
          "success", false,
          "message", "이미지 처리 중 오류가 발생했습니다.",
          "extractedIngredients", List.of(),
          "recipes", List.of()
      ));
    }
  }

  /**
   * 이미지에서 재료만 추출 (레시피 검색 없이)
   */
  @PostMapping("/extract-ingredients")
  public ResponseEntity<OCRResultDto> extractIngredientsOnly(
      @RequestParam("image") MultipartFile imageFile
  ) {
    try {
      List<String> extractedIngredients = ocrService.extractIngredientsFromImage(imageFile);

      return ResponseEntity.ok(
          OCRResultDto.success(extractedIngredients, "재료 추출이 완료되었습니다.")
      );

    } catch (IOException | TesseractException e) {
      log.error("재료 추출 중 오류 발생", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
          OCRResultDto.error("재료 추출 중 오류가 발생했습니다.")
      );
    }
  }

  /**
   * 재료 자동완성 기능 (기존)
   */
  @GetMapping("/ingredients/autocomplete")
  public List<String> autocompleteIngredients(
      @RequestParam String keyword,
      @RequestParam(defaultValue = "10") int limit
  ) {
    return searchService.searchIngredients(keyword, limit);
  }

  /**
   * 통합 재료 검색 - 텍스트 입력과 이미지 업로드를 모두 지원
   */
  @PostMapping("/search-combined")
  public ResponseEntity<?> searchRecipesCombined(
      @RequestParam(required = false) List<String> textIngredients,
      @RequestParam(required = false) MultipartFile image,
      @RequestParam(required = false) List<String> subIngredients,
      @RequestParam(defaultValue = "latest") String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    try {
      List<String> mainIngredients = new java.util.ArrayList<>();
      List<String> extractedIngredients = List.of();

      // 1. 텍스트로 입력된 재료 추가
      if (textIngredients != null && !textIngredients.isEmpty()) {
        mainIngredients.addAll(textIngredients);
      }

      // 2. 이미지에서 재료 추출하여 추가
      if (image != null && !image.isEmpty()) {
        extractedIngredients = ocrService.extractIngredientsFromImage(image);
        mainIngredients.addAll(extractedIngredients);
      }

      // 3. 메인 재료가 없으면 에러 반환
      if (mainIngredients.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of(
            "success", false,
            "message", "검색할 재료를 입력하거나 이미지를 업로드해주세요.",
            "extractedIngredients", extractedIngredients,
            "recipes", List.of()
        ));
      }

      // 4. 레시피 검색
      List<String> finalSubIngredients = subIngredients != null ? subIngredients : List.of();
      Pageable pageable = PageRequest.of(page, size);
      List<PostDto> recipes = searchService.searchRecipes(mainIngredients, finalSubIngredients,
          sort,
          pageable);

      return ResponseEntity.ok(Map.of(
          "success", true,
          "message", "검색이 완료되었습니다.",
          "textIngredients", textIngredients != null ? textIngredients : List.of(),
          "extractedIngredients", extractedIngredients,
          "mainIngredients", mainIngredients,
          "subIngredients", finalSubIngredients,
          "recipes", recipes,
          "recipeCount", recipes.size()
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