package SITE.RECIPICK.RECIPICK_PROJECT.dto;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.UserGrade;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GradeUpdateRequest {

  @Schema(example = "GOLD", description = "등급(BRONZE|SILVER|GOLD)")
  private String grade;

  public UserGrade toEnum() {
    try {
      return UserGrade.valueOf(grade.toUpperCase());
    } catch (Exception e) {
      throw new IllegalArgumentException("INVALID_GRADE");
    }
  }
}
