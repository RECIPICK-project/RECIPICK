package SITE.RECIPICK.RECIPICK_PROJECT.dto.admin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminDashboardResponse {

  private long totalUsers;
  private long activeUsers;
  private long totalPosts;
  private long officialPosts;
  private long reportedPosts;  // reportCount > 0
  private long reportedReviews;
  private long reportedComments;
}
