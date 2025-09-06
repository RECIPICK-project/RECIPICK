package SITE.RECIPICK.RECIPICK_PROJECT.dto;

// import 제거 - String으로 변경했으므로 불필요
// import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity.CookingCategory;
// import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity.CookingKind;  
// import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity.CookingMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
  @NotNull(message = "제목을 입력해주세요(예 : 오늘 저녁은 짜파게티다.) ")
  @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
  private String title;

  // 음식 제목
  @NotNull(message = "음식이름을 입력해주세요(짜파게티) ")
  @Size(max = 50, message = "음식명은 50자를 초과할 수 없습니다.")
  private String foodName;

  // ✅ 조리방법 - 필수 (String으로 받아서 Service에서 Enum으로 변환)
  @NotNull(message = "조리방법을 선택해주세요")
  private String ckgMth;

  // ✅ 카테고리 - 필수 (String으로 받아서 Service에서 Enum으로 변환)
  @NotNull(message = "요리 카테고리를 선택해주세요")
  private String ckgCategory;

  // ✅ 요리 종류 - 필수 (String으로 받아서 Service에서 Enum으로 변환)
  @NotNull(message = "요리 종류를 선택해주세요")
  private String ckgKnd;

  // ✅ 재료내용 - 필수, 빈 배열 불가
  @NotEmpty(message = "재료를 최소 1개 이상 입력해주세요")
  private List<String> ckgMtrlCn;

  // ✅ 몇 인분 - 필수 (Integer 타입으로 원복)
  @NotNull(message = "몇 인분인지 선택해주세요")
  private Integer ckgInbun;

  // ✅ 조리 난이도 - 필수 (Integer 타입으로 원복)
  @NotNull(message = "조리 난이도를 선택해주세요")
  private Integer ckgLevel;

  // ✅ 조리시간 - 필수 (Integer 타입으로 원복)
  @NotNull(message = "조리 시간을 선택해주세요")
  private Integer ckgTime;

  // ✅ 썸네일 이미지 URL - 선택적 (빈 문자열 허용)
  private String rcpImgUrl;

  // ✅ 조리 단계별 설명 - 필수, 빈 배열 불가
  @NotEmpty(message = "조리 과정을 최소 1단계 이상 입력해주세요")
  private List<String> rcpSteps;

  // 단계별 이미지 URLs (| 구분자로 저장)
  private List<String> rcpStepsImg;
}