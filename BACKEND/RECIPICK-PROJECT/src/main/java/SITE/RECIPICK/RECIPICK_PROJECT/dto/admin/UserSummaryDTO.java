package SITE.RECIPICK.RECIPICK_PROJECT.dto.admin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserSummaryDTO {

  private Integer userId;
  private String email;
  private boolean active;
  private String role; // ROLE_USER / ROLE_ADMIN
}
