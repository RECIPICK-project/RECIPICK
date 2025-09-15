package SITE.RECIPICK.RECIPICK_PROJECT.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReviewDto {
    private Integer userId;
    private Double rating;
    private String comment;
}
