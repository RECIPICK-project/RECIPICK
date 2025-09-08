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
public class SearchPostEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "post_id")
  private Integer postId;

  @Column(name = "user_id", nullable = false)
  private Integer userId;

  // ✅ 필수 필드로 변경
  @Column(name = "title", nullable = false, length = 200)
  private String title;

  // ✅ 필수 필드로 변경
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

  // ✅ 조리방법 - 필수 필드로 변경
  @Enumerated(EnumType.STRING)
  @Column(name = "ckg_mth", nullable = false, length = 100)
  private CookingMethod ckgMth;

//  // ✅ 카테고리 - 필수 필드로 변경
//  @Enumerated(EnumType.STRING)
//  @Column(name = "ckg_category", nullable = false, length = 100)
//  private CookingCategory ckgCategory;

  // ✅ 요리 종류 - 필수 필드로 변경
  @Enumerated(EnumType.STRING)
  @Column(name = "ckg_knd", nullable = false, length = 100)
  private CookingKind ckgKnd;

  // ✅ 재료내용 - 필수 필드로 변경
  @Column(name = "ckg_mtrl_cn", nullable = false, columnDefinition = "TEXT")
  private String ckgMtrlCn;

  // ✅ 몇 인분 - 필수 필드, 드롭다운 (1~10)
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "ckg_inbun", nullable = false)
  private CookingInbun ckgInbun;

  // ✅ 조리 난이도 - 필수 필드, 드롭다운 (1~5)
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "ckg_level", nullable = false)
  private CookingLevel ckgLevel;

  // ✅ 조리시간 - 필수 필드, 드롭다운 (10, 15, 20, 30, 90, 120)
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "ckg_time", nullable = false)
  private CookingTime ckgTime;

  // ✅ 썸네일 이미지 URL - 필수 필드로 변경
  @Column(name = "rcp_img_url", nullable = false, length = 500)
  private String rcpImgUrl;

  // ✅ 조리 단계별 설명 - 필수 필드로 변경
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

  // ✅ 몇 인분 enum (1~10)
  @Getter
  public enum CookingInbun {
    ONE(1, "1인분"),
    TWO(2, "2인분"),
    THREE(3, "3인분"),
    FOUR(4, "4인분"),
    FIVE(5, "5인분"),
    SIX(6, "6인분"),
    SEVEN(7, "7인분"),
    EIGHT(8, "8인분"),
    NINE(9, "9인분"),
    TEN(10, "10인분");

    private final int value;
    private final String description;

    CookingInbun(int value, String description) {
      this.value = value;
      this.description = description;
    }
  }

  // ✅ 조리 난이도 enum (1~5)
  @Getter
  public enum CookingLevel {
    LEVEL_1(1, "매우 쉬움"),
    LEVEL_2(2, "쉬움"),
    LEVEL_3(3, "보통"),
    LEVEL_4(4, "어려움"),
    LEVEL_5(5, "매우 어려움");

    private final int value;
    private final String description;

    CookingLevel(int value, String description) {
      this.value = value;
      this.description = description;
    }
  }

  // ✅ 조리시간 enum (10, 15, 20, 30, 90, 120분)
  @Getter
  public enum CookingTime {
    TIME_10(10, "10분"),
    TIME_15(15, "15분"),
    TIME_20(20, "20분"),
    TIME_30(30, "30분"),
    TIME_90(90, "1시간 30분"),
    TIME_120(120, "2시간");

    private final int minutes;
    private final String description;

    CookingTime(int minutes, String description) {
      this.minutes = minutes;
      this.description = description;
    }
  }

  // 조리방법 enum (기존)
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

  // 요리 카테고리 enum (기존)
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

  // 요리 종류 enum (기존)
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