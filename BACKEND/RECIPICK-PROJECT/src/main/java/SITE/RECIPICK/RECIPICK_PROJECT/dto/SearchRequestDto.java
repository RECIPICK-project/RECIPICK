package SITE.RECIPICK.RECIPICK_PROJECT.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchRequestDto {

  private String main;    // 메인 재료
  private String sub;     // 서브 재료
  private String sort;    // 정렬 기준: views, likes, latest
  private int page;       // 페이지 번호
  private int size;       // 페이지 사이즈
}
