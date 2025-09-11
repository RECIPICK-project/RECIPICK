package SITE.RECIPICK.RECIPICK_PROJECT.dto.admin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserSummaryDTO {

  private Integer userId;
  private String nicknameOrEmail; // 프론트가 보여줄 라벨
  private String email;           // 원하면 유지
  private boolean active;          // now<until 이면 false
  private String grade;           // BRONZE/SILVER/GOLD/PLATINUM/DIAMOND
  private Integer points;          // 선택
  private Integer reportCount;     // 선택
  private java.time.LocalDateTime createdAt; // 선택
  private java.time.LocalDateTime suspendedUntil; // 선택(프론트 표시에 유용)
  private String role;            // USER/ADMIN
}