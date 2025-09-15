package SITE.RECIPICK.RECIPICK_PROJECT.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * PROFILE - PK 공유: USER.user_id - grade: 혜택용 회원등급 (권한 아님)
 */
@Entity
@Getter
@Setter
@Table(name = "profile")
public class ProfileEntity {

  @Id
  @Column(name = "user_id")
  private Integer userId;

  @OneToOne
  @MapsId
  @JoinColumn(name = "user_id")
  private UserEntity userEntity;

  @Column(nullable = false, unique = true, length = 50)
  private String nickname;

  @Column(name = "profile_img", length = 500)
  private String profileImg;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserGrade grade = UserGrade.BRONZE;

  public void changeGrade(UserGrade grade) {
    this.grade = grade;
  }

}
