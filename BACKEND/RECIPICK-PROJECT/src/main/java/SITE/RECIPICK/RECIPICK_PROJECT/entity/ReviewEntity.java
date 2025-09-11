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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "REVIEW",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_post_review", columnNames = {"user_id", "post_id"})
    },
    indexes = {
        @Index(name = "idx_review_post", columnList = "post_id"),
        @Index(name = "idx_review_user", columnList = "user_id"),
        @Index(name = "idx_review_rating", columnList = "review_rating"),
        @Index(name = "idx_review_created", columnList = "created_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "review_id", updatable = false, nullable = false, columnDefinition = "INT UNSIGNED")
  private Long id;

  /**
   * 게시글
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", nullable = false,
      foreignKey = @ForeignKey(name = "fk_review_post"))
  private PostEntity post;

  /**
   * 작성자
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false,
      foreignKey = @ForeignKey(name = "fk_review_user"))
  private UserEntity user;

  /**
   * 평점 (0.00 ~ 5.00)
   */
  @Column(name = "review_rating", nullable = false, precision = 3, scale = 2)
  private BigDecimal rating;

  /**
   * 리뷰 내용
   */
  @Lob
  @Column(name = "comment")
  private String comment;

  /**
   * 신고 횟수
   */
  @Builder.Default
  @Column(name = "report_count", columnDefinition = "INT UNSIGNED DEFAULT 0")
  private Integer reportCount = 0;

  /**
   * 작성일
   */
  @Column(name = "created_at", updatable = false, insertable = false,
      columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
  private LocalDateTime createdAt;

  /**
   * 수정일
   */
  @Column(name = "updated_at", insertable = false,
      columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
  private LocalDateTime updatedAt;
}
