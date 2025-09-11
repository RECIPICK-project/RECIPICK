package SITE.RECIPICK.RECIPICK_PROJECT.dto.admin;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserActiveRequest {

  private boolean active; // true=활성, false=정지
}
