package SITE.RECIPICK.RECIPICK_PROJECT.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="review")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "review_id", updatable = false, nullable = false, columnDefinition = "INT UNSIGNED")
  private Integer id;

  /**
   * 게시글
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", nullable = false)
  private PostEntity post;

  /**
   * 작성자
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  /**
   * 평점 (0.00 ~ 5.00)
   */
  @Column(name = "review_rating", nullable = false, precision = 3, scale = 2)
  private BigDecimal rating;

  /**
   * 리뷰 내용
   */
  @Column(name = "comment", columnDefinition = "TEXT", nullable = false)
  private String comment;

  @Builder.Default
  @Column(name = "report_count", nullable = false)
  private int reportCount = 0; // 신고 횟수 (기본값 0)

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt; // 작성일시

  @Column(name = "updated_at")
  private LocalDateTime updatedAt; // 수정일시

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
