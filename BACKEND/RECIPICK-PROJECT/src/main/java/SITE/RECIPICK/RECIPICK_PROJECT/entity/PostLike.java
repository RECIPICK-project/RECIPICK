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
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * LIKE_TABLE 매핑 엔티티
 * <p>
 * ✅ 설계 의도 - PK: like_id (AUTO_INCREMENT) - FK1: user_id → USER.user_id (다대일) - FK2: post_id →
 * POST.post_id (다대일) - createdAt: 좋아요 누른 시점 기록
 * <p>
 * ⚠️ 주의 - 테이블 이름은 ERD 기준으로 LIKE_TABLE (대문자) - 한 유저가 한 게시글에 여러 번 좋아요 못 누르도록 하려면 UNIQUE 제약 필요
 * (user_id + post_id)
 */
@Entity
@Getter
@Setter
@Table(name = "LIKE_TABLE")
public class PostLike {

  // ===== PK =====
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)  // AUTO_INCREMENT
  @Column(name = "like_id")
  private Integer id;                                  // 좋아요 ID

  // ===== 연관관계 =====
  @ManyToOne(fetch = FetchType.LAZY)                  // 다:1 관계 (여러 Like → 한 User)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;                                   // 좋아요 누른 유저

  @ManyToOne(fetch = FetchType.LAZY)                  // 다:1 관계 (여러 Like → 한 Post)
  @JoinColumn(name = "post_id", nullable = false)
  private Post post;                                   // 좋아요가 눌린 게시글

  // ===== 필드 =====
  @Column(name = "created_at")
  private LocalDateTime createdAt;                     // 좋아요 누른 시각

  // ===== 라이프사이클 훅 =====
  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();                 // INSERT 시점 자동 기록
    }
  }
}
