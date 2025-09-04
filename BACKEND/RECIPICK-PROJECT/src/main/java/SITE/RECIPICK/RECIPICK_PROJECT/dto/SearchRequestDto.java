package SITE.RECIPICK.RECIPICK_PROJECT.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchRequestDto {

  private List<String> mainIngredients;  // 메인 재료 리스트로 변경
  private List<String> subIngredients;   // 서브 재료 리스트로 변경
  private String sort = "latest";        // 정렬 기준: views, likes, latest (기본값)
  private int page = 0;                  // 페이지 번호 (기본값)
  private int size = 20;                 // 페이지 사이즈 (기본값)

  // 문자열로 받은 경우를 위한 편의 메서드들
  public void setMain(String main) {
    if (main != null && !main.trim().isEmpty()) {
      this.mainIngredients = List.of(main.split(","));
    }
  }

  public void setSub(String sub) {
    if (sub != null && !sub.trim().isEmpty()) {
      this.subIngredients = List.of(sub.split(","));
    }
  }
}