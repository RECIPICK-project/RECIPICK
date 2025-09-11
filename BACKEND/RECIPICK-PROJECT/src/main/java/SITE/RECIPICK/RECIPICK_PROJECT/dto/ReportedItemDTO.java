package SITE.RECIPICK.RECIPICK_PROJECT.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 신고 Top 목록 응답용 (선택: 필요 시 사용)
 */
@Getter
@AllArgsConstructor
public class ReportedItemDTO {

  private Long targetId;
  private long count;
}
