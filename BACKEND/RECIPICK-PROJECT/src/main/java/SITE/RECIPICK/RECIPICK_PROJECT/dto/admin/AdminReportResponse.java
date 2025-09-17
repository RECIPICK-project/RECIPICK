package SITE.RECIPICK.RECIPICK_PROJECT.dto.admin;

import java.time.LocalDateTime;

public record AdminReportResponse(
    Integer id,
    String targetType, // "POST", "COMMENT" …
    Integer targetId,
    String reason,
    String status, // "PENDING", "ACCEPTED", "REJECTED"
    String reporter, // 닉네임 or 이메일
    String targetPreview, // 레시피 제목 / 댓글 일부
    LocalDateTime createdAt) {

}
