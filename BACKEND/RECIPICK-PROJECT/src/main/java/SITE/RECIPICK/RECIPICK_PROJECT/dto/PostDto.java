package SITE.RECIPICK.RECIPICK_PROJECT.dto;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity.CookingCategory;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity.CookingKind;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity.CookingMethod;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostDto {

  // 글 제목
  private String title;
  // 음식 제목
  private String foodName;
  // 조리방법 (드롭다운)
  private CookingMethod ckgMth;
  // 카테고리 (드롭다운)
  private CookingCategory ckgCategory;
  // 요리 종류 (드롭다운)
  private CookingKind ckgKnd;
  // 재료내용 (각 div박스에서 입력받은 재료들을 | 구분자로 저장)
  private String ckgMtrlCn;
  // 몇 인분
  private Integer ckgInbun;
  // 조리 난이도 (1~5)
  private Integer ckgLevel;
  // 조리시간 (분 단위)
  private Integer ckgTime;
  // 썸네일 이미지 URL
  private String rcpImgUrl;
  // 조리 단계별 설명 (| 구분자로 저장)
  private List<String> rcpSteps;
  // 단계별 이미지 URLs (| 구분자로 저장)
  private List<String> rcpStepsImg;

}
