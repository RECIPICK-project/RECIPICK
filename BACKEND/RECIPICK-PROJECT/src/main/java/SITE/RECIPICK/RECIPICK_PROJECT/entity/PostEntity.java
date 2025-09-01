package SITE.RECIPICK.RECIPICK_PROJECT.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "POST")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "post_id")
  private Long postId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "title", nullable = false, length = 200)
  private String title;

  @Column(name = "food_name", nullable = false, length = 100)
  private String foodName;

  @Column(name = "view_count")
  @Builder.Default
  private Integer viewCount = 0;

  @Column(name = "like_count")
  @Builder.Default
  private Integer likeCount = 0;

  // 조리방법 (드롭다운)
  @Enumerated(EnumType.STRING)
  @Column(name = "ckg_mth", length = 100)
  private CookingMethod ckgMth;

  // 카테고리 (드롭다운)
  @Enumerated(EnumType.STRING)
  @Column(name = "ckg_category", length = 100)
  private CookingCategory ckgCategory;

  // 요리 종류 (드롭다운)
  @Enumerated(EnumType.STRING)
  @Column(name = "ckg_knd", length = 100)
  private CookingKind ckgKnd;

  // 재료내용 (각 div박스에서 입력받은 재료들을 | 구분자로 저장)
  @Column(name = "ckg_mtrl_cn", columnDefinition = "TEXT")
  private String ckgMtrlCn;

  // 몇 인분
  @Column(name = "ckg_inbun")
  private Integer ckgInbun;

  // 조리 난이도 (1~5)
  @Column(name = "ckg_level")
  private Integer ckgLevel;

  // 조리시간 (분 단위)
  @Column(name = "ckg_time")
  private Integer ckgTime;

  // 썸네일 이미지 URL
  @Column(name = "rcp_img_url", length = 500)
  private String rcpImgUrl;

  // 조리 단계별 설명 (| 구분자로 저장)
  @Column(name = "rcp_steps", columnDefinition = "TEXT")
  private String rcpSteps;

  // 단계별 이미지 URLs (| 구분자로 저장, 선택사항)
  @Column(name = "rcp_steps_img", columnDefinition = "TEXT")
  private String rcpStepsImg;

  // 정식 레시피 여부 (0: 임시, 1: 정식)
  @Column(name = "rcp_is_official", columnDefinition = "TINYINT DEFAULT 0")
  @Builder.Default
  private Integer rcpIsOfficial = 0;

  // 신고 횟수
  @Column(name = "report_count")
  @Builder.Default
  private Integer reportCount = 0;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
  
  // 조리방법 enum
  @Getter
  public enum CookingMethod {
    BOILING("끓이기"),
    GRILLING("굽기"),
    FRYING("튀기기"),
    STIR_FRYING("볶기"),
    STEAMING("찌기"),
    BRAISING("조리기"),
    ROASTING("구이"),
    SIMMERING("졸이기"),
    BLANCHING("데치기"),
    SEASONING("무침"),
    RAW("생식"),
    OTHER("기타");

    private final String description;

    CookingMethod(String description) {
      this.description = description;
    }
  }

  // 요리 카테고리 enum
  @Getter
  public enum CookingCategory {
    KOREAN("한식"),
    CHINESE("중식"),
    JAPANESE("일식"),
    WESTERN("양식"),
    ASIAN("아시안"),
    DESSERT("디저트"),
    BEVERAGE("음료"),
    SALAD("샐러드"),
    SOUP("국물요리"),
    SIDE_DISH("반찬"),
    MAIN_DISH("메인요리"),
    SNACK("간식"),
    OTHER("기타");

    private final String description;

    CookingCategory(String description) {
      this.description = description;
    }
  }

  // 요리 종류 enum
  @Getter
  public enum CookingKind {
    RICE("밥류"),
    NOODLE("면류"),
    SOUP("국/탕"),
    STEW("찌개"),
    SIDE_DISH("반찬"),
    KIMCHI("김치"),
    MEAT("육류요리"),
    SEAFOOD("해산물요리"),
    VEGETABLE("채소요리"),
    BREAD("빵/베이커리"),
    DESSERT("후식/디저트"),
    BEVERAGE("음료"),
    SAUCE("소스/양념"),
    PICKLE("절임/장아찌"),
    OTHER("기타");

    private final String description;

    CookingKind(String description) {
      this.description = description;
    }
  }
}