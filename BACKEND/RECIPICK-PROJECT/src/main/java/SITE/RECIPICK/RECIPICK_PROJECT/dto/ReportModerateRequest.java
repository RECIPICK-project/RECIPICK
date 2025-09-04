package SITE.RECIPICK.RECIPICK_PROJECT.dto;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.ReportStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * 신고 처리 요청 action: ACCEPT | REJECT
 */
@Getter
@Setter
public class ReportModerateRequest {

  private String action; // ACCEPT or REJECT
  private String status; // enum 변환용 필드

  // 서비스 단에서 enum 변환에 사용할 상태 문자열 제공
  public String getStatus() {
    if ("ACCEPT".equalsIgnoreCase(action)) {
      return ReportStatus.ACCEPTED.name();
    } else if ("REJECT".equalsIgnoreCase(action)) {
      return ReportStatus.REJECTED.name();
    } else {
      throw new IllegalArgumentException("Invalid action: " + action);
    }
  }
}
