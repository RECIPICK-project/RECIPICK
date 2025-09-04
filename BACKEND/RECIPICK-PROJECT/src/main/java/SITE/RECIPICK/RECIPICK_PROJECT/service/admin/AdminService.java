package SITE.RECIPICK.RECIPICK_PROJECT.service.admin;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDTO;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.ReportCreateRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.ReportModerateRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.admin.AdminDashboardResponse;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.admin.UserSummaryDTO;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.ReportEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.ReportStatus;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.ReportTargetType;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.UserEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.CommentRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.PostRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.ReportRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.ReviewRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.UserRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.util.PostMapper;
import java.time.LocalDate;
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
  private final CommentRepository commentRepo;
  private final ReportRepository reportRepo;

  @Transactional(readOnly = true)
  public AdminDashboardResponse getDashboard(int days, int minReports, int top) {
    var now = LocalDate.now();
    var fromDate = now.minusDays(days - 1).atStartOfDay();
    var toDate = now.plusDays(1).atStartOfDay(); // between [from, to)

    long totalUsers = userRepo.count();
    long totalRecipes = postRepo.count();

    // 임계치 이상 신고 누적 레시피 수 (레시피 테이블의 reportCount 사용)
    long reportedRecipesOverThreshold = postRepo.countByReportCountGreaterThanEqual(minReports);

    // 방문자 추이 (데이터 소스 없으면 빈 시리즈)
    var visitorSeries = new AdminDashboardResponse.Series(
        java.util.stream.IntStream.range(0, days)
            .mapToObj(i -> now.minusDays(days - 1 - i))
            .toList(),
        java.util.stream.IntStream.range(0, days)
            .mapToObj(i -> 0L) // 추후 연동 시 실제 값
            .toList()
    );

    // 카테고리별 업로드 (기간 내)
    var catAgg = postRepo.countByCategoryBetween(fromDate, toDate); // 아래 레포 쿼리 참조
    var categoryUploads = catAgg.stream()
        .map(a -> new AdminDashboardResponse.CategoryCount(a.getCategory(), a.getCnt()))
        .toList();

    // 최근 신고 top개
    var recentReports = reportRepo.findTopNByOrderByCreatedAtDesc(top).stream()
        .map(r -> new AdminDashboardResponse.RecentReportItem(
            r.getId(),
            r.getTargetType().name(),
            r.getTargetId(),
            r.getReason(),
            r.getStatus().name(),
            r.getCreatedAt().toLocalDate()
        )).toList();

    // 최근 가입 top개
    var recentSignups = userRepo.findTopNByOrderByCreatedAtDesc(top).stream()
        .map(u -> new AdminDashboardResponse.RecentSignupItem(
            u.getId(),
            safeNickOrEmail(u), // 닉네임 없으면 이메일
            u.getCreatedAt().toLocalDate()
        )).toList();

    return new AdminDashboardResponse(
        totalUsers,
        totalRecipes,
        reportedRecipesOverThreshold,
        visitorSeries,
        categoryUploads,
        recentReports,
        recentSignups
    );
  }

  private String safeNickOrEmail(UserEntity u) {
    var pr = u.getProfileEntity(); // 없으면 null 허용
    return (pr != null && pr.getNickname() != null) ? pr.getNickname() : u.getEmail();
  }

  // === User 관리 ===
  @Transactional(readOnly = true)
  public java.util.List<UserSummaryDTO> listUsers(int offset, int limit) {
    var page = PageRequest.of(offset / Math.max(1, limit), Math.max(1, limit));
    return userRepo.findAllByOrderByIdDesc(page).stream()
        .map(u -> UserSummaryDTO.builder()
            .userId(u.getId())
            .email(u.getEmail())
            .active(u.isActive())
            .role(u.getRole())
            .build())
        .toList();
  }

  @Transactional
  public void updateUserRole(Integer userId, String role) {
    var u = userRepo.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
    if (!"ROLE_USER".equals(role) && !"ROLE_ADMIN".equals(role)) {
      throw new IllegalArgumentException("INVALID_ROLE");
    }
    u.changeRole(role);
  }

  @Transactional
  public void updateUserActive(Integer userId, boolean active) {
    var u = userRepo.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
    u.setActive(active);
  }

  // === Post 관리 ===
  public List<PostDTO> listPendingPosts(int offset, int limit) {
    var page = PageRequest.of(offset / Math.max(1, limit), Math.max(1, limit));
    return postRepo.findByRcpIsOfficialFalseOrderByCreatedAtDesc(page)
        .stream()
        .map(PostMapper::toDto)
        .toList();
  }


  @Transactional
  public void publishPost(Long postId) {
    var p = postRepo.findById(postId)
        .orElseThrow(() -> new IllegalArgumentException("POST_NOT_FOUND"));
    p.publish(); // rcpIsOfficial=true
  }

  @Transactional
  public void deletePost(Long postId) {
    // 운영정책에 따라 soft delete 필요하면 별도 플래그로 처리
    postRepo.deleteById(postId);
  }

  // === 신고 많은 항목 ===
  @Transactional(readOnly = true)
  public java.util.List<SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDTO> topReportedPosts(int min,
      int offset, int limit) {
    var page = PageRequest.of(offset / Math.max(1, limit), Math.max(1, limit));
    return postRepo.findByReportCountGreaterThanOrderByReportCountDesc(min, page)
        .stream().map(SITE.RECIPICK.RECIPICK_PROJECT.util.PostMapper::toDto).toList();
  }

  @Transactional
  public void deleteReview(Integer reviewId) {
    reviewRepo.deleteById(reviewId);
  }

  @Transactional
  public void deleteComment(Integer commentId) {
    commentRepo.deleteById(commentId);
  }

  // ===== 신고 등록 =====
  @Transactional
  public void createReport(Integer reporterUserId, ReportCreateRequest req) {
    if (req.getTargetType() == null || req.getTargetId() == null ||
        req.getReason() == null || req.getReason().isBlank()) {
      throw new IllegalArgumentException("REPORT_INVALID_REQUEST");
    }

    ReportTargetType type;
    try {
      type = ReportTargetType.valueOf(
          req.getTargetType().trim().toUpperCase()); // POST/REVIEW/COMMENT/USER
    } catch (Exception e) {
      throw new IllegalArgumentException("INVALID_TARGET_TYPE");
    }

    ReportEntity r = new ReportEntity();
    r.setTargetType(type);
    r.setTargetId(req.getTargetId());       // USER 신고 시 USER.user_id
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

  @Transactional
  public void moderate(Long id, ReportModerateRequest req) {
    var r = reportRepo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("REPORT_NOT_FOUND"));

    // DTO가 action/status 중 무엇을 보내든 우선순위 정해서 하나만 고름
    String raw = (req.getStatus() != null && !req.getStatus().isBlank())
        ? req.getStatus()
        : req.getAction(); // action만 온 케이스 허용

    String enumName = normalizeReportStatusName(raw);
    ReportStatus newStatus = ReportStatus.valueOf(enumName);

    r.setStatus(newStatus);
    // 필요하면 시간/처리자 기록 등 추가
  }


  private String normalizeReportStatusName(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("INVALID_STATUS");
    }
    String s = raw.trim().toUpperCase();

    // enum 이름들(실제로 프로젝트에서 사용하는 ReportStatusEntity.values())을 런타임에 조회
    java.util.Set<String> names = java.util.Arrays.stream(ReportStatus.values())
        .map(Enum::name)
        .collect(java.util.stream.Collectors.toSet());

    // 승인 계열
    if (s.equals("ACCEPT") || s.equals("ACCEPTED") || s.equals("RESOLVE") || s.equals("RESOLVED")) {
      // 프로젝트 enum에 무엇이 있든 그걸로 맞춰줌
      if (names.contains("ACCEPTED")) {
        return "ACCEPTED";
      }
      if (names.contains("RESOLVED")) {
        return "RESOLVED";
      }
    }

    // 거절/기각 계열
    if (s.equals("REJECT") || s.equals("REJECTED") || s.equals("DISMISS") || s.equals(
        "DISMISSED")) {
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

}