package SITE.RECIPICK.RECIPICK_PROJECT.dto;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/** 신고 처리 요청 action: ACCEPT | REJECT */
@Getter
@Setter
public class ReportModerateRequest {

    private String action; // ACCEPT or REJECT

    @Schema(hidden = true)
    public ReportStatus toEnum() {
        if ("ACCEPT".equalsIgnoreCase(action)) {
            return ReportStatus.ACCEPTED;
        } else if ("REJECT".equalsIgnoreCase(action)) {
            return ReportStatus.REJECTED;
        } else {
            throw new IllegalArgumentException("INVALID_ACTION");
        }
    }
}
