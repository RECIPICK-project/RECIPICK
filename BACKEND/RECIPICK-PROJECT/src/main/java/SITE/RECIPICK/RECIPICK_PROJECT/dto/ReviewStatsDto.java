package SITE.RECIPICK.RECIPICK_PROJECT.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewStatsDto {

  private BigDecimal averageRating;
  private Long totalReviews;
  private Long ratingCount1;
  private Long ratingCount2;
  private Long ratingCount3;
  private Long ratingCount4;
  private Long ratingCount5;
}