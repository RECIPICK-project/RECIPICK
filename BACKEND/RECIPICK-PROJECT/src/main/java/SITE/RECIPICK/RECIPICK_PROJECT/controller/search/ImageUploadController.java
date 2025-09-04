package SITE.RECIPICK.RECIPICK_PROJECT.controller.search;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.search.OCRResultDto;
import SITE.RECIPICK.RECIPICK_PROJECT.service.search.OCRService;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class ImageUploadController {

  // 지원하는 이미지 파일 형식
  private static final List<String> SUPPORTED_FORMATS = List.of(
      "image/jpeg", "image/jpg", "image/png", "image/bmp", "image/tiff"
  );
  // 최대 파일 크기 (10MB)
  private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
  private final OCRService ocrService;

  /**
   * 영수증 이미지에서 재료 추출
   */
  @PostMapping("/extract-ingredients")
  public ResponseEntity<?> extractIngredients(@RequestParam("image") MultipartFile imageFile) {
    try {
      // 파일 검증
      String validationError = validateImageFile(imageFile);
      if (validationError != null) {
        return ResponseEntity.badRequest().body(
            OCRResultDto.error(validationError)
        );
      }

      log.info("이미지 OCR 처리 시작: {} ({}KB)",
          imageFile.getOriginalFilename(),
          imageFile.getSize() / 1024);

      // OCR 처리 및 재료 추출
      List<String> extractedIngredients = ocrService.extractIngredientsFromImage(imageFile);

      log.info("OCR 처리 완료: {} 개 재료 추출됨", extractedIngredients.size());

      return ResponseEntity.ok(
          OCRResultDto.success(extractedIngredients, "재료 추출이 완료되었습니다.")
      );

    } catch (IOException e) {
      log.error("이미지 파일 처리 중 오류 발생", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
          OCRResultDto.error("이미지 파일을 처리하는 중 오류가 발생했습니다.")
      );
    } catch (TesseractException e) {
      log.error("OCR 처리 중 오류 발생", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
          OCRResultDto.error("텍스트 인식 중 오류가 발생했습니다.")
      );
    } catch (Exception e) {
      log.error("예상치 못한 오류 발생", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
          OCRResultDto.error("처리 중 오류가 발생했습니다.")
      );
    }
  }

  /**
   * OCR 처리 상태 확인용 헬스체크
   */
  @GetMapping("/health")
  public ResponseEntity<String> healthCheck() {
    return ResponseEntity.ok("OCR Service is running");
  }

  /**
   * 이미지 파일 검증
   */
  private String validateImageFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return "이미지 파일이 비어있습니다.";
    }

    // 파일 크기 체크
    if (file.getSize() > MAX_FILE_SIZE) {
      return "파일 크기가 너무 큽니다. 10MB 이하의 파일을 업로드해주세요.";
    }

    // 파일 형식 체크
    String contentType = file.getContentType();
    if (contentType == null || !SUPPORTED_FORMATS.contains(contentType.toLowerCase())) {
      return "지원하지 않는 파일 형식입니다. JPG, PNG, BMP, TIFF 파일을 업로드해주세요.";
    }

    // 파일명 체크
    String originalFilename = file.getOriginalFilename();
    if (originalFilename == null || originalFilename.trim().isEmpty()) {
      return "유효하지 않은 파일명입니다.";
    }

    return null; // 검증 통과
  }
}