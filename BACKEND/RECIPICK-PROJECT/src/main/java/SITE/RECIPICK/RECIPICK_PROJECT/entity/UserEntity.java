package SITE.RECIPICK.RECIPICK_PROJECT.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * USER 테이블 매핑 엔티티
 * <p>
 * ✅ 설계 의도 - PK: user_id (AUTO_INCREMENT) - 로그인 정보: email, password - 계정 상태: active(활성 여부), stop(정지
 * 횟수), role(권한) - 시각 기록: createdAt, latestAt
 * <p>
 * ⚠️ 주의 - password는 반드시 암호화된 값으로만 setPassword() 해야 함. - role은 ROLE_USER / ROLE_ADMIN 등으로 제한적으로 사용.
 * - @PrePersist 훅으로 최초 생성 시점 자동 기록.
 */

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
  private Boolean active = true;

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
  @Column(name = "suspended_until")
  private LocalDateTime suspendedUntil;  // null이면 정지 아님
  @Column(name = "suspended_reason")
  private String suspendedReason;        // 선택

  // === 상태 변경 메서드 ===

  @PrePersist
  public void onCreate() {
    this.createdAt = LocalDateTime.now();
    this.latestAt = LocalDateTime.now();
    if (this.stop == null) {
      this.stop = 0;
    }
    if (this.active == null) {
      this.active = false; // 가입시 이메일 인증 전이므로 false
    }
    if (this.role == null) {
      this.role = "ROLE_USER";
    }
    if (this.provider == null) {
      this.provider = "LOCAL"; // 기본은 일반 회원가입
    }
  }

  @PreUpdate
  public void onUpdate() {
    this.latestAt = LocalDateTime.now();
  }

  /**
   * 이메일 변경
   */
  public void changeEmail(String email) {
    this.email = email;
  }

  /**
   * 비밀번호 변경 (⚠ 반드시 암호화된 값으로 세팅해야 함)
   */
  public void changePassword(String password) {
    this.password = password;
  }

  /**
   * 계정 활성/비활성 상태 변경
   */
  public void changeActive(boolean active) {
    this.active = active;
  }

  /**
   * 정지 횟수 +1
   */
  public void increaseStop() {
    if (stop == null) {
      stop = 0;
    }
    stop++;
  }

  /**
   * 역할(권한) 변경 → ROLE_USER / ROLE_ADMIN
   */
  public void changeRole(String role) {
    this.role = role;
  }

  /**
   * 최근 접속 시각 갱신
   */
  public void updateLatestAt() {
    this.latestAt = LocalDateTime.now();
  }
}
