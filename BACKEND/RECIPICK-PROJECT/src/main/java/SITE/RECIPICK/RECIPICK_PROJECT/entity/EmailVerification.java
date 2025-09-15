package SITE.RECIPICK.RECIPICK_PROJECT.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "email_verification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    // 메일에는 평문 코드를 보내고 DB에는 해시만 저장
    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt;

    // ✅ primitive boolean으로 두면 isUsed() 게터가 생기고 null 이슈 없음
    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    // 발송 쿨다운/일일 한도용
    @Column(name = "last_sent_at")
    private LocalDateTime lastSentAt;

    @Column(name = "last_sent_date")
    private LocalDate lastSentDate;

    @Column(name = "send_count_today")
    private Integer sendCountToday;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
