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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * REVIEW 테이블 매핑
 * <p>
 * ✅ 핵심 - PK: review_id (AUTO_INCREMENT) - FK: post_id  → POST.post_id (N:1) - FK: user_id  →
 * USER.user_id (N:1) - 평점: decimal(3,2) = 0.00 ~ 9.99 범위 표현 가능. (비즈니스적으로 1.00~5.00만 허용하고 싶으면 서비스에서
 * 검증) - 코멘트: TEXT - 신고횟수: int (기본 0) - 생성/수정시각: @PrePersist / @PreUpdate 로 자동 기록
 * <p>
 * ⚠️ 주의 - BigDecimal은 null 가능 → 서비스에서 null 차단 또는 기본값 로직 넣는 게 안전. - FK 지연 로딩(LAZY) 사용 → 트랜잭션 밖에서
 * 접근하면 LazyInitializationException 가능.
 */
@Entity
@Getter
@Setter
@Table(name = "REVIEW")
public class Review {

  // ====== PK ======
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL AUTO_INCREMENT
  @Column(name = "review_id")
  private Integer id;

  // ====== 연관관계 ======
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", nullable = false)  // REVIEW.post_id → POST.post_id
  private Post post;                               // 대상 게시글

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)  // REVIEW.user_id → USER.user_id
  private User author;                             // 작성자

  // ====== 컬럼 ======
  @Column(name = "review_rating", precision = 3, scale = 2, nullable = false)
  private BigDecimal rating; // 예: 4.50 (서비스/컨트롤러 단에서 1.00~5.00 범위 검증 권장)

  @Column(name = "comment", columnDefinition = "TEXT")
  private String comment;    // 리뷰 내용

  @Column(name = "report_count")
  private Integer reportCount = 0; // 신고 횟수(기본 0)

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  // ====== 라이프사이클 훅 ======
  // INSERT/UPDATE 직전에 시간/기본값을 안전하게 채워준다.
  @PrePersist
  void prePersist() {
    LocalDateTime now = LocalDateTime.now();
    if (createdAt == null) {
      createdAt = now;
    }
    if (updatedAt == null) {
      updatedAt = now;
    }
    if (reportCount == null) {
      reportCount = 0;
    }
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = LocalDateTime.now();
  }

  // ====== 비즈니스 메서드 ======

  /**
   * 신고 누적 +1 (null 방어 포함)
   */
  public void increaseReport() {
    if (reportCount == null) {
      reportCount = 0;
    }
    reportCount++;
  }
}
