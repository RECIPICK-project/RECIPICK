// AdminDashboardResponse.java
package SITE.RECIPICK.RECIPICK_PROJECT.dto.admin;

import java.time.LocalDate;
import java.util.List;

public record AdminDashboardResponse(
    long totalUsers,
    long totalRecipes,
    long reportedRecipesOverThreshold,
    // Chart.js용 추이 데이터
    Series visitorTrend,
    // 카테고리별 업로드(막대/도넛)
    List<CategoryCount> categoryUploads,
    // 테이블 섹션
    List<RecentReportItem> recentReports,
    List<RecentSignupItem> recentSignups
) {

  public record Series(
      List<LocalDate> labels,
      List<Long> data
  ) {

  }

  public record CategoryCount(
      String category, long count
  ) {

  }

  public record RecentReportItem(
      Long id, String targetType, Long targetId, String reason, String status, LocalDate createdAt
  ) {

  }

  public record RecentSignupItem(
      Integer userId, String nicknameOrEmail, LocalDate createdAt
  ) {

  }
}
