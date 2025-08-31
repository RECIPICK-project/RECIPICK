package SITE.RECIPICK.RECIPICK_PROJECT.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IngredientResponseDto {

  private Integer ingId;
  private String name;
  private String sort;
}
