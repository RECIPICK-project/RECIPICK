// 신고 생성
package SITE.RECIPICK.RECIPICK_PROJECT.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportCreateRequest {

  private String targetType; // POST/REVIEW/COMMENT/USER (대소문자 무시)
  private Integer targetId;
  private String reason;
}
