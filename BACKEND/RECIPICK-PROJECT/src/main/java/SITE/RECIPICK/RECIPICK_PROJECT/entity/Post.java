package SITE.RECIPICK.RECIPICK_PROJECT.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * POST 테이블 매핑 엔티티
 * <p>
 * ✅ 설계 의도 - PK: post_id (AUTO_INCREMENT) - 작성자: USER.user_id(FK) → 다:1 관계 (여러 Post가 하나의 User를 가리킴)
 * - 집계 필드: viewCount / likeCount / reportCount - 정식 여부: rcpIsOfficial (boolean) → 임시(false) /
 * 정식(true) - 시각: @PrePersist/@PreUpdate 훅으로 생성/수정 시점 자동 기록
 * <p>
 * ⚠️ 주의 - DB 컬럼명이 snake_case이므로 반드시 @Column(name="...") 매핑 필요 - 지연 로딩(LAZY) 사용: 트랜잭션 밖에서 접근 시
 * LazyInitializationException 발생 가능
 */
@Entity
@Getter
@Setter
@Table(name = "POST")
public class Post {

  // ===== 기본 키(PK) =====
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)   // MySQL AUTO_INCREMENT
  @Column(name = "post_id")                             // DB 컬럼명 명시
  private Long postId;                                  // 게시글 ID

  // ===== 연관 관계 =====
  @ManyToOne(fetch = FetchType.LAZY)                   // 다:1 관계 (N Posts → 1 User)
  @JoinColumn(name = "user_id", nullable = false)      // FK (null 불가)
  private User user;                                   // 작성자(User 엔티티 참조)

  // ===== 도메인 필드 =====
  @Column(name = "rcp_sno", length = 100)
  private String rcpSno;                               // 레시피 일련번호

  @Column(name = "title", length = 200, nullable = false)
  private String title;                                // 제목

  @Column(name = "food_name", length = 100, nullable = false)
  private String foodName;                             // 음식명

  @Column(name = "view_count", nullable = false)
  private int viewCount = 0;                           // 조회수 (기본 0)

  @Column(name = "like_count", nullable = false)
  private int likeCount = 0;                           // 좋아요 수 (기본 0)

  @Column(name = "ckg_mth", length = 100)
  private String ckgMth;                               // 조리 방법

  @Column(name = "ckg_category", length = 100)
  private String ckgCategory;                          // 카테고리

  @Column(name = "ckg_knd", length = 100)
  private String ckgKnd;                               // 종류

  @Column(name = "ckg_mtrl_cn", columnDefinition = "TEXT")
  private String ckgMtrlCn;                            // 재료 내용

  @Column(name = "ckg_inbun")
  private int ckgInbun;                                // 인분

  @Column(name = "ckg_level")
  private int ckgLevel;                                // 난이도

  @Column(name = "ckg_time")
  private int ckgTime;                                 // 조리 시간 (분 단위)

  @Column(name = "rcp_img_url", length = 500)
  private String rcpImgUrl;                            // 대표 이미지 URL

  @Column(name = "rcp_steps", columnDefinition = "TEXT")
  private String rcpSteps;                             // 조리 단계

  @Column(name = "rcp_steps_img", columnDefinition = "TEXT")
  private String rcpStepsImg;                          // 단계별 이미지 URL

  @Column(name = "rcp_is_official", nullable = false)
  private boolean rcpIsOfficial = false;               // 정식 여부 (false=임시, true=정식)

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;                     // 생성 시각

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;                     // 수정 시각

  @Column(name = "report_count", nullable = false)
  private int reportCount = 0;                         // 신고 횟수

  // ===== 엔티티 라이프사이클 훅 =====
  @PrePersist
  void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    if (createdAt == null) {
      createdAt = now;
    }
    if (updatedAt == null) {
      updatedAt = now;
    }
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  // ===== 비즈니스 메서드 =====

  /**
   * 조회수 +1
   */
  public void increaseView() {
    this.viewCount++;
  }

  /**
   * 좋아요 +1
   */
  public void increaseLike() {
    this.likeCount++;
  }

  /**
   * 좋아요 -1 (0 이하 방지)
   */
  public void decreaseLike() {
    if (this.likeCount > 0) {
      this.likeCount--;
    }
  }

  /**
   * 임시 → 정식 승격
   */
  public void publish() {
    this.rcpIsOfficial = true;
  }

  /**
   * 신고 +1
   */
  public void increaseReport() {
    this.reportCount++;
  }
}
