package SITE.RECIPICK.RECIPICK_PROJECT.entity;

import java.time.LocalDateTime;

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

import lombok.Getter;

/**
 * COMMENT 테이블 매핑 엔티티
 *
 * <p>✔ 매핑 정보 - PK: comment_id (AUTO_INCREMENT) - FK: post_id → POST.post_id - FK: user_id →
 * USER.user_id
 *
 * <p>✔ 컬럼 - content: 댓글 본문 (TEXT) - reportCount: 신고 횟수 (기본 0) - createdAt: 작성 시각 (insert 시 자동 세팅) -
 * updatedAt: 수정 시각 (insert/update 시 자동 갱신)
 *
 * <p>✔ JPA 생명주기 훅(@PrePersist, @PreUpdate)으로 날짜 자동 세팅
 */
@Entity
@Getter
@Table(name = "COMMENT")
public class CommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id; // 댓글 ID (PK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private PostEntity postEntity; // 댓글이 달린 게시글 (FK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity author; // 댓글 작성자 (FK)

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // 댓글 본문

    @Column(name = "report_count", nullable = false)
    private int reportCount = 0; // 신고 횟수 (기본값 0)

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // 작성일시

    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 수정일시

    // === 자동 세팅 ===
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
