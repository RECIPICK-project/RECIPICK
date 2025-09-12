package SITE.RECIPICK.RECIPICK_PROJECT.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRequestDto {

  private Integer postId;
  private BigDecimal rating;
  private String comment;
}