package SITE.RECIPICK.RECIPICK_PROJECT.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 255) // 소셜 로그인 시 비밀번호가 없을 수 있음
    private String password;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active =true;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column(nullable = false)
    private Integer stop;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime latestAt;

    @Column(nullable = false, length = 50)
    private String role; // ex) ROLE_USER, ROLE_ADMIN

    // 🔹 추가: 로그인 제공자 (LOCAL, GOOGLE, NAVER 등)
    @Column(nullable = false, length = 50)
    private String provider;


    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.latestAt = LocalDateTime.now();
        if (this.stop == null) this.stop = 0;
        if (this.active == null) this.active = false; // 가입시 이메일 인증 전이므로 false
        if (this.role == null) this.role = "ROLE_USER";
        if (this.provider == null) this.provider = "LOCAL"; // 기본은 일반 회원가입
    }

    @PreUpdate
    public void onUpdate() {
        this.latestAt = LocalDateTime.now();
    }
}