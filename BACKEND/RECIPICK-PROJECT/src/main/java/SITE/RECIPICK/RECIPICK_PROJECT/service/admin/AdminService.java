package SITE.RECIPICK.RECIPICK_PROJECT.service.admin;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.ReportCreateRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.ReportModerateRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.admin.AdminDashboardResponse;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.admin.UserSummaryDTO;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.ReportEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.ReportStatus;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.ReportTargetType;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.UserEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.PostRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.ReportRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.ReviewRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.UserRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.util.PostMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

  private final UserRepository userRepo;
  private final PostRepository postRepo;
  private final ReviewRepository reviewRepo;
  //    private final CommentRepository commentRepo;
  private final ReportRepository reportRepo;

  @Transactional(readOnly = true)
  public AdminDashboardResponse getDashboard(int days, int minReports, int top) {
    // ===== 공통 집계 =====
    var now = LocalDate.now(); // 기존 코드 유지 (카테고리 집계 등에서 사용)
    var fromDate = now.minusDays(days - 1).atStartOfDay();
    var toDate = now.plusDays(1).atStartOfDay();

    long totalUsers = userRepo.count();
    long totalRecipes = postRepo.count();

    // 임계치 이상 신고 누적 레시피 수 (레시피 테이블의 reportCount 사용)
    long reportedRecipesOverThreshold = postRepo.countByReportCountGreaterThanEqual(minReports);

    // ===== 방문자 추이(DAU) - KST 기준, DISTINCT userId =====
    // DB가 UTC 저장이라면: KST 경계를 UTC LocalDateTime으로 변환해서 between 조회
    var KST = java.time.ZoneId.of("Asia/Seoul");
    var todayKst = java.time.LocalDate.now(KST);

    var labels = new java.util.ArrayList<java.time.LocalDate>(days);
    var data = new java.util.ArrayList<Long>(days);

    for (int i = days - 1; i >= 0; i--) {
      var day = todayKst.minusDays(i); // KST의 해당 일자
      var zStart = day.atStartOfDay(KST);                              // KST 00:00:00
      var zEnd = day.plusDays(1).atStartOfDay(KST).minusNanos(1);      // KST 23:59:59.999999999

      // DB 타임스탬프가 UTC라면 UTC로 변환해서 질의
      var startUtc = java.time.LocalDateTime.ofInstant(zStart.toInstant(),
          java.time.ZoneOffset.UTC);
      var endUtc = java.time.LocalDateTime.ofInstant(zEnd.toInstant(), java.time.ZoneOffset.UTC);

      long dau = userRepo.countDistinctActiveBetween(startUtc, endUtc);

      labels.add(day);               // LocalDate → JSON에서 "yyyy-MM-dd"로 직렬화됨
      data.add(dau);
    }

    var visitorSeries = new AdminDashboardResponse.Series(labels, data);

    // ===== 카테고리별 업로드 (기간 내) =====
    var catAgg = postRepo.countByCategoryBetween(fromDate, toDate); // 기존 그대로
    var categoryUploads =
        catAgg.stream()
            .map(a -> new AdminDashboardResponse.CategoryCount(
                a.getCategory() == null ? null : a.getCategory().getDescription(),
                a.getCnt()))
            .toList();

    // ===== 최근 신고 top개 =====
    var recentReports =
        reportRepo.findTopNByOrderByCreatedAtDesc(top).stream()
            .map(r -> new AdminDashboardResponse.RecentReportItem(
                r.getId(),
                r.getTargetType().name(),
                r.getTargetId(),
                r.getReason(),
                r.getStatus().name(),
                r.getCreatedAt().toLocalDate()))
            .toList();

    // ===== 최근 가입 top개 =====
    var recentSignups =
        userRepo.findTopNByOrderByCreatedAtDesc(top).stream()
            .map(u -> new AdminDashboardResponse.RecentSignupItem(
                u.getUserId(),
                safeNickOrEmail(u), // 닉네임 없으면 이메일
                u.getCreatedAt().toLocalDate()))
            .toList();

    return new AdminDashboardResponse(
        totalUsers,
        totalRecipes,
        reportedRecipesOverThreshold,
        visitorSeries,
        categoryUploads,
        recentReports,
        recentSignups);
  }


  private String safeNickOrEmail(UserEntity u) {
    String nick = u.getNickname();
    return (nick != null && !nick.isBlank()) ? nick : u.getEmail();
  }

  // === User 관리 ===
  @Transactional(readOnly = true)
  public java.util.List<UserSummaryDTO> listUsers(int offset, int limit) {
    var page = PageRequest.of(offset / Math.max(1, limit), Math.max(1, limit));
    return userRepo.findAllByOrderByUserIdDesc(page).stream()
        .map(
            u ->
                UserSummaryDTO.builder()
                    .userId(u.getUserId())
                    .email(u.getEmail())
                    .active(u.getActive())
                    .role(u.getRole())
                    .build())
        .toList();
  }

  @Transactional
  public void updateUserRole(Integer userId, String role) {
    var u =
        userRepo.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
    if (!"ROLE_USER".equals(role) && !"ROLE_ADMIN".equals(role)) {
      throw new IllegalArgumentException("INVALID_ROLE");
    }
    u.changeRole(role);
  }

  @Transactional
  public void updateUserActive(Integer userId, boolean active) {
    var u =
        userRepo.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
    u.setActive(active);
  }

  // === Post 관리 ===
  public List<PostDto> listPendingPosts(int offset, int limit) {
    var page = PageRequest.of(offset / Math.max(1, limit), Math.max(1, limit));
    return postRepo.findByRcpIsOfficialOrderByCreatedAtDesc(0, page).stream()
        .map(PostMapper::toDto)
        .toList();
  }

  @Transactional
  public void publishPost(Integer postId) {
    var p =
        postRepo.findById(postId)
            .orElseThrow(() -> new IllegalArgumentException("POST_NOT_FOUND"));
    p.setRcpIsOfficial(1);
  }

  @Transactional
  public void deletePost(Integer postId) {
    // 운영정책에 따라 soft delete 필요하면 별도 플래그로 처리
    postRepo.deleteById(postId);
  }

  // === 신고 많은 항목 ===
  @Transactional(readOnly = true)
  public java.util.List<SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDto> topReportedPosts(
      int min, int offset, int limit) {
    var page = PageRequest.of(offset / Math.max(1, limit), Math.max(1, limit));
    return postRepo.findByReportCountGreaterThanOrderByReportCountDesc(min, page).stream()
        .map(SITE.RECIPICK.RECIPICK_PROJECT.util.PostMapper::toDto)
        .toList();
  }

  @Transactional
  public void deleteReview(Integer reviewId) {
    reviewRepo.deleteById(reviewId);
  }

//    @Transactional
//    public void deleteComment(Integer commentId) {
//        commentRepo.deleteById(commentId);
//    }

  // ===== 신고 등록 =====
  @Transactional
  public void createReport(Integer reporterUserId, ReportCreateRequest req) {
    if (req.getTargetType() == null
        || req.getTargetId() == null
        || req.getReason() == null
        || req.getReason().isBlank()) {
      throw new IllegalArgumentException("REPORT_INVALID_REQUEST");
    }

    ReportTargetType type;
    try {
      type =
          ReportTargetType.valueOf(
              req.getTargetType().trim().toUpperCase()); // POST/REVIEW/COMMENT/USER
    } catch (Exception e) {
      throw new IllegalArgumentException("INVALID_TARGET_TYPE");
    }

    ReportEntity r = new ReportEntity();
    r.setTargetType(type);
    r.setTargetId(req.getTargetId()); // USER 신고 시 USER.user_id
    r.setReason(req.getReason().trim());
    r.setStatus(ReportStatus.PENDING);

    reportRepo.save(r);
  }

  @Transactional(readOnly = true)
  public Page<ReportEntity> listReports(String status, String type, int page, int size) {
    var pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));

    ReportStatus st;
    try {
      st = ReportStatus.valueOf(status == null ? "PENDING" : status.trim().toUpperCase());
    } catch (Exception e) {
      throw new IllegalArgumentException("INVALID_STATUS");
    }

    if (type == null || type.isBlank()) {
      return reportRepo.findByStatus(st, pageable);
    } else {
      ReportTargetType tt;
      try {
        tt = ReportTargetType.valueOf(type.trim().toUpperCase()); // USER 가능
      } catch (Exception e) {
        throw new IllegalArgumentException("INVALID_TARGET_TYPE");
      }
      return reportRepo.findByStatusAndTargetType(st, tt, pageable);
    }
  }

  // ===== 신고 처리 =====
  @Transactional
  public void moderate(Integer id, ReportModerateRequest req) {
    var r =
        reportRepo
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("REPORT_NOT_FOUND"));

    if (req == null) {
      throw new IllegalArgumentException("REQUEST_REQUIRED");
    }

    // DTO에서 enum 변환(ACCEPT -> ACCEPTED, REJECT -> REJECTED)
    var newStatus = req.toEnum(); // 여기서 INVALID_ACTION 등 IllegalArgumentException 던질 수 있음

    r.setStatus(newStatus);
    // 필요시 r.setModeratedAt(LocalDateTime.now()); 등 추가
  }

  private String normalizeReportStatusName(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("INVALID_STATUS");
    }
    String s = raw.trim().toUpperCase();

    // enum 이름들(실제로 프로젝트에서 사용하는 ReportStatusEntity.values())을 런타임에 조회
    java.util.Set<String> names =
        java.util.Arrays.stream(ReportStatus.values())
            .map(Enum::name)
            .collect(java.util.stream.Collectors.toSet());

    // 승인 계열
    if (s.equals("ACCEPT")
        || s.equals("ACCEPTED")
        || s.equals("RESOLVE")
        || s.equals("RESOLVED")) {
      // 프로젝트 enum에 무엇이 있든 그걸로 맞춰줌
      if (names.contains("ACCEPTED")) {
        return "ACCEPTED";
      }
      if (names.contains("RESOLVED")) {
        return "RESOLVED";
      }
    }

    // 거절/기각 계열
    if (s.equals("REJECT")
        || s.equals("REJECTED")
        || s.equals("DISMISS")
        || s.equals("DISMISSED")) {
      if (names.contains("REJECTED")) {
        return "REJECTED";
      }
      if (names.contains("DISMISSED")) {
        return "DISMISSED";
      }
    }

    // 보류/대기 같은 것도 들어오면 허용 (선택)
    if (s.equals("PENDING")) {
      if (names.contains("PENDING")) {
        return "PENDING";
      }
    }

    throw new IllegalArgumentException("INVALID_STATUS");
  }

  public void suspendUser(Integer userId, LocalDateTime until, String reason) {
    var u = userRepo.findById(userId).orElseThrow();
    if (until == null) { // 해제
      u.setSuspendedUntil(null);
      u.setSuspendedReason(null);
    } else {
      if (until.isBefore(LocalDateTime.now())) {
        throw new IllegalArgumentException("UNTIL_IN_PAST");
      }
      u.setSuspendedUntil(until);
      u.setSuspendedReason(reason);
    }
    userRepo.save(u);
  }
}
