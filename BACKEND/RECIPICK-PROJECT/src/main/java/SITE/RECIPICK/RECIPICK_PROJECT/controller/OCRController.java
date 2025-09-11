package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.SearchPostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.service.OCRService;
import SITE.RECIPICK.RECIPICK_PROJECT.service.SearchService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // 개발용, 운영환경에서는 특정 도메인만 허용
public class OCRController {

  private final OCRService ocrService;
  private final SearchService searchService;

  /**
   * 영수증 OCR 텍스트 추출 (재료만)
   */
  @PostMapping("/extract")
  public ResponseEntity<Map<String, Object>> extractIngredients(
      @RequestParam("image") MultipartFile imageFile
  ) {
    try {
      // 파일 검증
      if (imageFile == null || imageFile.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of(
            "success", false,
            "message", "이미지 파일이 필요합니다."
        ));
      }

      log.info("OCR 추출 요청 - 파일명: {}, 크기: {} bytes",
          imageFile.getOriginalFilename(), imageFile.getSize());

      // OCR 처리
      List<String> extractedIngredients = ocrService.extractIngredientsFromImage(imageFile);

      return ResponseEntity.ok(Map.of(
          "success", true,
          "message", "OCR 처리 완료",
          "filename", imageFile.getOriginalFilename(),
          "fileSize", imageFile.getSize(),
          "extractedIngredients", extractedIngredients,
          "ingredientCount", extractedIngredients.size()
      ));

    } catch (Exception e) {
      log.error("OCR 추출 중 오류 발생", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
          "success", false,
          "message", "OCR 처리 중 오류가 발생했습니다: " + e.getMessage()
      ));
    }
  }

  /**
   * 영수증 OCR + 레시피 검색 통합 엔드포인트
   */
  @PostMapping("/search")
  public ResponseEntity<Map<String, Object>> ocrAndSearch(
      @RequestParam("image") MultipartFile imageFile,
      @RequestParam(required = false) List<String> additionalMain,
      @RequestParam(required = false) List<String> additionalSub,
      @RequestParam(defaultValue = "latest") String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    List<String> extractedIngredients = new ArrayList<>();
    List<String> mainIngredients =
        additionalMain != null ? new ArrayList<>(additionalMain) : new ArrayList<>();
    List<String> subIngredients =
        additionalSub != null ? new ArrayList<>(additionalSub) : new ArrayList<>();

    try {
      // 1. OCR 처리
      if (imageFile != null && !imageFile.isEmpty()) {
        log.info("통합 OCR 검색 시작 - 파일: {}, 크기: {} bytes",
            imageFile.getOriginalFilename(), imageFile.getSize());

        extractedIngredients = ocrService.extractIngredientsFromImage(imageFile);

        if (!extractedIngredients.isEmpty()) {
          // OCR로 추출된 재료들을 메인 재료로 추가
          mainIngredients.addAll(extractedIngredients);
          log.info("OCR 추출된 재료: {}", extractedIngredients);
        } else {
          log.warn("OCR 처리 결과 재료 추출 실패");
        }
      }

      // 2. 검색할 재료가 없으면 OCR 결과만 반환
      if (mainIngredients.isEmpty()) {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", extractedIngredients.isEmpty() ?
                "영수증에서 재료를 찾을 수 없습니다." :
                "재료는 추출되었지만 검색 조건이 없습니다.",
            "extractedIngredients", extractedIngredients,
            "mainIngredients", mainIngredients,
            "subIngredients", subIngredients,
            "recipes", List.of(),
            "recipeCount", 0
        ));
      }

      // 3. 레시피 검색 실행
      Pageable pageable = PageRequest.of(page, size);
      Map<String, Object> searchResult = searchService.searchRecipes(
          mainIngredients, subIngredients, sort, pageable);

      @SuppressWarnings("unchecked")
      List<SearchPostDto> recipes = (List<SearchPostDto>) searchResult.get("recipes");
      Integer totalCount = (Integer) searchResult.get("totalCount");

      log.info("통합 OCR 검색 완료 - 추출 재료: {}개, 검색 결과: {}건",
          extractedIngredients.size(), totalCount);

      return ResponseEntity.ok(Map.of(
          "success", true,
          "message", String.format("검색 완료! %s",
              extractedIngredients.isEmpty() ? "" :
                  "영수증에서 " + extractedIngredients.size() + "개 재료를 인식했습니다."),
          "extractedIngredients", extractedIngredients,
          "mainIngredients", mainIngredients,
          "subIngredients", subIngredients,
          "recipes", recipes,
          "recipeCount", totalCount,
          "currentPage", page,
          "pageSize", size
      ));

    } catch (Exception e) {
      log.error("통합 OCR 검색 중 오류 발생", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
          "success", false,
          "message", "OCR 검색 처리 중 오류가 발생했습니다: " + e.getMessage(),
          "extractedIngredients", extractedIngredients,
          "mainIngredients", mainIngredients,
          "subIngredients", subIngredients,
          "recipes", List.of(),
          "recipeCount", 0
      ));
    }
  }

  /**
   * OCR 원본 텍스트 확인 (디버깅용)
   */
  @PostMapping("/debug")
  public ResponseEntity<Map<String, Object>> debugOCR(
      @RequestParam("image") MultipartFile imageFile
  ) {
    try {
      if (imageFile == null || imageFile.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of(
            "success", false,
            "message", "이미지 파일이 필요합니다."
        ));
      }

      log.info("OCR 디버그 요청 - 파일: {}", imageFile.getOriginalFilename());

      // 실제로는 OCRService에 디버그 메소드를 추가해야 합니다
      List<String> extractedIngredients = ocrService.extractIngredientsFromImage(imageFile);

      return ResponseEntity.ok(Map.of(
          "success", true,
          "message", "OCR 디버그 정보",
          "filename", imageFile.getOriginalFilename(),
          "fileSize", imageFile.getSize(),
          "extractedIngredients", extractedIngredients,
          "note", "원본 텍스트 확인을 위해서는 OCRService에 디버그 메소드 추가 필요"
      ));

    } catch (Exception e) {
      log.error("OCR 디버그 중 오류 발생", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
          "success", false,
          "message", "OCR 디버그 실패: " + e.getMessage()
      ));
    }
  }

  /**
   * OCR 서비스 상태 확인
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, Object>> healthCheck() {
    try {
      // 기본적인 서비스 상태 확인
      return ResponseEntity.ok(Map.of(
          "success", true,
          "message", "OCR 서비스 정상 작동 중",
          "timestamp", System.currentTimeMillis(),
          "service", "네이버 클로바 OCR",
          "endpoints", Map.of(
              "extract", "POST /api/ocr/extract - 재료 추출만",
              "search", "POST /api/ocr/search - OCR + 레시피 검색",
              "debug", "POST /api/ocr/debug - 디버깅 정보",
              "health", "GET /api/ocr/health - 상태 확인"
          )
      ));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
          "success", false,
          "message", "OCR 서비스 오류: " + e.getMessage()
      ));
    }
  }
}