package SITE.RECIPICK.RECIPICK_PROJECT.entity;

import SITE.RECIPICK.RECIPICK_PROJECT.converter.CookingCategoryConverter;
import SITE.RECIPICK.RECIPICK_PROJECT.converter.CookingKindConverter;
import SITE.RECIPICK.RECIPICK_PROJECT.converter.CookingMethodConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
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
  private Integer postId;

  @Column(name = "user_id", nullable = false)
  private Integer userId;

  @Column(name = "email", length = 255)  // user_email → email
  private String userEmail;

  @Column(name = "nickname", length = 100)  // user_nickname → nickname
  private String userNickname;

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

  @Column(name = "rcp_is_official", columnDefinition = "TINYINT DEFAULT 0")
  @Builder.Default
  private Integer rcpIsOfficial = 0;

  @Column(name = "report_count")
  @Builder.Default
  private Integer reportCount = 0;

  // ✅ 조리방법 - 한글로 저장 ("굽기", "끓이기" 등)
  @Convert(converter = CookingMethodConverter.class)
  @Column(name = "ckg_mth", nullable = false, length = 100)
  private CookingMethod ckgMth;

  // ✅ 카테고리 - 한글로 저장 ("가공식품류", "건어물류" 등)
  @Convert(converter = CookingCategoryConverter.class)
  @Column(name = "ckg_category", nullable = false, length = 100)
  private CookingCategory ckgCategory;

  // ✅ 요리 종류 - 한글로 저장 ("과자", "국/탕" 등)
  @Convert(converter = CookingKindConverter.class)
  @Column(name = "ckg_knd", nullable = false, length = 100)
  private CookingKind ckgKnd;

  @Column(name = "ckg_mtrl_cn", nullable = false, columnDefinition = "TEXT")
  private String ckgMtrlCn;

  // ✅ 몇 인분 - 숫자로 저장 (1, 2, 3, 4, 5, 6) - 원래대로 Integer
  @Column(name = "ckg_inbun", nullable = false)
  private Integer ckgInbun;

  // ✅ 조리 난이도 - 숫자로 저장 (1, 2, 3, 4, 5) - 원래대로 Integer
  @Column(name = "ckg_level", nullable = false)
  private Integer ckgLevel;


  // ✅ 조리시간 - 숫자로 저장 (5, 10, 15, 30, 60, 90, 120, 121) - 원래대로 Integer
  @Column(name = "ckg_time", nullable = false)
  private Integer ckgTime;


  @Column(name = "rcp_img_url", nullable = false, length = 500)
  private String rcpImgUrl;

  @Column(name = "rcp_steps", nullable = false, columnDefinition = "TEXT")
  private String rcpSteps;

  @Column(name = "rcp_steps_img", columnDefinition = "TEXT")
  private String rcpStepsImg;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  // ✅ 몇 인분 enum (1~6) - value로 저장
  @Getter
  public enum CookingInbun {
    ONE(1, "1인분"),
    TWO(2, "2인분"),
    THREE(3, "3인분"),
    FOUR(4, "4인분"),
    FIVE(5, "5인분"),
    SIX(6, "6인분 이상");

    private final Integer value;
    private final String description;

    CookingInbun(Integer value, String description) {
      this.value = value;
      this.description = description;
    }
  }

  // ✅ 조리 난이도 enum (1~5) - value로 저장
  @Getter
  public enum CookingLevel {
    LEVEL_1(1, "매우 쉬움"),
    LEVEL_2(2, "쉬움"),
    LEVEL_3(3, "보통"),
    LEVEL_4(4, "어려움"),
    LEVEL_5(5, "매우 어려움");

    private final Integer value;
    private final String description;

    CookingLevel(Integer value, String description) {
      this.value = value;
      this.description = description;
    }
  }

  // ✅ 조리시간 enum - minutes로 저장
  @Getter
  public enum CookingTime {
    TIME_5(5, "5분이내"),
    TIME_10(10, "10분이내"),
    TIME_15(15, "15분이내"),
    TIME_30(30, "30분이내"),
    TIME_60(60, "60분이내"),
    TIME_90(90, "90분이내"),
    TIME_120(120, "2시간이내"),
    TIME_121(121, "2시간이상");

    private final Integer minutes;
    private final String description;

    CookingTime(Integer minutes, String description) {
      this.minutes = minutes;
      this.description = description;
    }
  }

  // ✅ 조리방법 enum - description으로 저장
  @Getter
  public enum CookingMethod {
    GRILLING("굽기"),
    OTHER("기타"),
    BOILING("끓이기"),
    BLANCHING("데치기"),
    SEASONING("무침"),
    STIR_FRYING("볶음"),
    PAN_FRYING("부침"),
    MIXING("비빔"),
    SIMMERING("삶기"),
    PICKLING("절임"),
    BRAISING("조림"),
    STEAMING("찜"),
    FRYING("튀김"),
    RAW("회");

    private final String description;

    CookingMethod(String description) {
      this.description = description;
    }
  }

  // ✅ 요리 카테고리 enum - description으로 저장
  @Getter
  public enum CookingCategory {
    PROCESSED_FOOD("가공식품류"),
    DRIED_SEAFOOD("건어물류"),
    GRAINS("곡류"),
    FRUITS("과일류"),
    OTHER("기타"),
    EGG_DAIRY("달걀/유제품"),
    CHICKEN("닭고기"),
    PORK("돼지고기"),
    FLOUR("밀가루"),
    MUSHROOM("버섯류"),
    BEEF("소고기"),
    RICE("쌀"),
    MEAT("육류"),
    VEGETABLES("채소류"),
    BEANS_NUTS("콩/견과류"),
    SEAFOOD("해물류");

    private final String description;

    CookingCategory(String description) {
      this.description = description;
    }
  }

  // ✅ 요리 종류 enum - description으로 저장
  @Getter
  public enum CookingKind {
    SNACK("과자"),
    SOUP("국/탕"),
    OTHER("기타"),
    KIMCHI_JEOTGAL("김치/젓갈/장류"),
    DESSERT("디저트"),
    MAIN_SIDE_DISH("메인반찬"),
    NOODLE_DUMPLING("면/만두"),
    SIDE_DISH("밑반찬"),
    RICE_PORRIDGE("밥/죽/떡"),
    BREAD("빵"),
    SALAD("샐러드"),
    SOUP_WESTERN("스프"),
    SAUCE_JAM("양념/소스/잼"),
    WESTERN("양식"),
    STEW("찌개"),
    BEVERAGE("차/음료/술"),
    FUSION("퓨전");

    private final String description;

    CookingKind(String description) {
      this.description = description;
    }
  }
}
