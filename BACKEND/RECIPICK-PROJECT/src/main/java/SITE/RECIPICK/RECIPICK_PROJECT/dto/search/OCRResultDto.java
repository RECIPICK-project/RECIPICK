package SITE.RECIPICK.RECIPICK_PROJECT.dto.search;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OCRResultDto {

  private boolean success;
  private String message;
  private List<String> ingredients;
  private int ingredientCount;
  private LocalDateTime processedAt;

  /**
   * 성공 응답 생성
   */
  public static OCRResultDto success(List<String> ingredients, String message) {
    OCRResultDto dto = new OCRResultDto();
    dto.success = true;
    dto.message = message;
    dto.ingredients = ingredients;
    dto.ingredientCount = ingredients != null ? ingredients.size() : 0;
    dto.processedAt = LocalDateTime.now();
    return dto;
  }

  /**
   * 실패 응답 생성
   */
  public static OCRResultDto error(String errorMessage) {
    OCRResultDto dto = new OCRResultDto();
    dto.success = false;
    dto.message = errorMessage;
    dto.ingredients = List.of();
    dto.ingredientCount = 0;
    dto.processedAt = LocalDateTime.now();
    return dto;
  }
}