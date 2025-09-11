package SITE.RECIPICK.RECIPICK_PROJECT.dto.admin;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SuspendUserRequest {

  private LocalDateTime until; // null 이면 해제(=즉시 활성)
  private String reason;       // 선택
}
