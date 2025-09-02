package SITE.RECIPICK.RECIPICK_PROJECT.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
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
@Getter
@Setter
@Table(name = "USER")
public class User {

  // === 계정 상태 ===

  @Column(nullable = false)
  private boolean active = true;   // 계정 활성 여부 (true=정상, false=정지)

  @Column(nullable = false)
  private Integer stop = 0;        // 정지 횟수 (경고 누적용)

  @Column(nullable = false, length = 25)
  private String role = "ROLE_USER"; // 기본 ROLE_USER

  // === 기본키 ===
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)   // MySQL AUTO_INCREMENT
  @Column(name = "user_id")
  private Integer id;

  // === 로그인 정보 ===
  @Column(nullable = false, unique = true, length = 255)
  private String email;

  @Column(nullable = false, length = 255)
  private String password;

  // === 시간 정보 ===
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "latest_at")
  private LocalDateTime latestAt;

  // === 라이프사이클 콜백 ===
  @PrePersist
  void prePersist() {
    LocalDateTime now = LocalDateTime.now();
    if (createdAt == null) {
      createdAt = now;
    }
    if (latestAt == null) {
      latestAt = now;
    }
  }

  // === 상태 변경 메서드 ===

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
