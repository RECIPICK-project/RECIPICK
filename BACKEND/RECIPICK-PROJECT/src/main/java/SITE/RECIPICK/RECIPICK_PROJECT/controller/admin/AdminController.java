package SITE.RECIPICK.RECIPICK_PROJECT.controller.admin;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.GradeUpdateRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.ReportCreateRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.ReportModerateRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.admin.AdminDashboardResponse;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.admin.SuspendUserRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.admin.UpdateUserActiveRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.admin.UserSummaryDTO;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.ReportEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.UserRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.service.admin.AdminGradeService;
import SITE.RECIPICK.RECIPICK_PROJECT.service.admin.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * AdminController
 * <p>
 * ✅ 관리자 전용 API 엔드포인트 집합 - 모든 경로는 "/admin" 하위 - 서비스 계층(AdminService, AdminGradeService)로 위임하여 비즈니스
 * 로직 수행 - 현재 로그인한 관리자 식별은 Spring Security Authentication + UserRepository로 처리
 * <p>
 * ⚠️ 주의 - "임시 하드코딩된 사용자 ID"는 사용하지 않음. - 신고 등록 등 "신고자 ID"가 필요한 곳에서는 Authentication의 이름(email)로 DB 조회
 * → userId 추출.
 */

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "admin", description = "관리자 전용 API")
public class AdminController {

  private final AdminService svc;           // 게시글/신고/유저 등 관리자 핵심 기능
  private final AdminGradeService gradeSvc; // 유저 등급 변경 전담
  private final UserRepository userRepo;    // 현재 인증된 사용자(email) → userId 조회용

  // ===================== Users =====================


  /**
   * 유저 목록(최신순) - offset/limit 페이지네이션
   */

  @GetMapping("/users")
  @Operation(summary = "유저 목록", description = "최신순, offset/limit 페이지네이션")
  public List<UserSummaryDTO> listUsers(
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit
  ) {
    return svc.listUsers(offset, limit);
  }


  /**
   * 유저 등급 변경 (BRONZE | SILVER | GOLD) - 요청 바디의 grade 값은 대소문자 무시, 서비스에서 Enum으로 검증/치환 - 성공 시 204 No
   * Content
   */

  @PatchMapping(value = "/users/{userId}/grade", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "유저 등급 변경", description = "등급을 BRONZE | SILVER | GOLD 중 하나로 변경합니다.")
  public ResponseEntity<Void> updateUserGrade(
      @PathVariable Integer userId,
      @Valid @RequestBody GradeUpdateRequest req
  ) {
    gradeSvc.updateUserGrade(userId, req);
    return ResponseEntity.noContent().build();
  }


  /**
   * 유저 활성/정지 상태 변경 - active=true  → 활성 - active=false → 정지
   */

  @PatchMapping("/users/{userId}/active")
  @Operation(summary = "유저 활성/정지", description = "active=true/false")
  public void updateUserActive(
      @PathVariable Integer userId,
      @RequestBody UpdateUserActiveRequest req
  ) {
    svc.updateUserActive(userId, req.isActive());
  }

  // ===================== Posts =====================


  /**
   * 미승인(임시 저장) 레시피 목록
   */

  @GetMapping("/posts/pending")
  @Operation(summary = "미승인(임시) 레시피 목록")
  public List<PostDto> pendingPosts(
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit
  ) {
    return svc.listPendingPosts(offset, limit);
  }


  /**
   * 임시 레시피 → 정식 레시피 전환
   */

  @PostMapping("/posts/{postId}/publish")
  @Operation(summary = "레시피 정식 전환")
  public void publishPost(@PathVariable Integer postId) {
    svc.publishPost(postId);
  }


  /**
   * 레시피 삭제
   */

  @DeleteMapping("/posts/{postId}")
  @Operation(summary = "레시피 삭제")
  public void deletePost(@PathVariable Integer postId) {
    svc.deletePost(postId);
  }

  // ===================== Reports =====================


  /**
   * 신고 누적 상위 레시피 - reportCount > min
   */

  @GetMapping("/reports/posts")
  @Operation(summary = "신고 많은 레시피", description = "reportCount > min")
  public List<PostDto> reportedPosts(
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "0") int min
  ) {
    return svc.topReportedPosts(min, offset, limit);
  }


  /**
   * 리뷰 삭제 (관리자)
   */

  @DeleteMapping("/reports/reviews/{reviewId}")
  @Operation(summary = "리뷰 삭제")
  public void deleteReview(@PathVariable Integer reviewId) {
    svc.deleteReview(reviewId);
  }


  /**
   * 댓글 삭제 (관리자)
   */

  @DeleteMapping("/reports/comments/{commentId}")
  @Operation(summary = "댓글 삭제")
  public void deleteComment(@PathVariable Integer commentId) {
    svc.deleteComment(commentId);
  }


  /**
   * 신고 등록 (일반 사용자도 사용하는 엔드포인트) - Authentication에서 email을 가져와 DB로 사용자 조회 → 신고자 ID로 사용 - 운영 단계에서는
   * /reports 로 경로 이동 가능(현재는 관리 탭에서 테스트 용이하게 /admin 하위에 둠)
   */

  @PostMapping("/reports")
  @Operation(summary = "신고 등록")
  public void createReport(
      Authentication authentication,        // SecurityContext의 인증 정보
      @Valid @RequestBody ReportCreateRequest req
  ) {
    Integer reporterId = currentUserId(authentication);
    svc.createReport(reporterId, req);
  }


  /**
   * 신고 목록 조회 - status : PENDING | ACCEPTED | REJECTED - type   : POST | REVIEW | COMMENT | USER
   * (nullable → 전체)
   */

  @GetMapping("/reports")
  @Operation(summary = "신고 목록 조회",
      description = "status=PENDING|ACCEPTED|REJECTED, type=POST|REVIEW|COMMENT|USER")
  public Page<ReportEntity> listReports(
      @RequestParam(defaultValue = "PENDING") String status,
      @RequestParam(required = false) String type,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    return svc.listReports(status, type, page, size);
  }


  /**
   * 신고 처리 (관리자) - action=ACCEPT → ACCEPTED - action=REJECT → REJECTED
   */

  @PatchMapping("/reports/{id}")
  @Operation(summary = "신고 처리", description = "action=ACCEPT|REJECT")
  public void moderate(@PathVariable Long id, @RequestBody ReportModerateRequest req) {
    svc.moderate(id, req);
  }

  // 기간 정지/해제
  @PatchMapping("/users/{userId}/suspend")
  @Operation(summary = "유저 기간 정지/해제",
      description = "until(ISO-8601) 기준으로 정지. null이면 해제")
  public void suspendUser(
      @PathVariable Integer userId,
      @RequestBody SuspendUserRequest req
  ) {
    svc.suspendUser(userId, req.getUntil(), req.getReason()); // until==null -> 해제
  }

  // ===================== Dashboard =====================


  /**
   * 관리자 대시보드 통계 - days       : 최근 N일 기준(기본 7) - minReports : 신고 누적 최소 기준(기본 3) - top        : 상위
   * N개(기본 5)
   */

  @GetMapping("/dashboard")
  @Operation(summary = "대시보드 통계 조회")
  public AdminDashboardResponse dashboard(
      @RequestParam(defaultValue = "7") int days,
      @RequestParam(defaultValue = "3") int minReports,
      @RequestParam(defaultValue = "5") int top
  ) {
    return svc.getDashboard(days, minReports, top);
  }

  // ===================== Helpers =====================


  /**
   * 현재 인증 사용자 ID 조회 - authentication.getName() → 일반적으로 username(email) - email로 DB 조회하여
   * UserEntity.id 반환 - 인증 또는 사용자 조회 실패 시 IllegalStateException
   */

  private Integer currentUserId(Authentication authentication) {
    if (authentication == null || authentication.getName() == null) {
      throw new IllegalStateException("UNAUTHENTICATED");
    }
    var email = authentication.getName();
    var user = userRepo.findByEmail(email)
        .orElseThrow(() -> new IllegalStateException("AUTH_USER_NOT_FOUND"));
    return user.getUserId();
  }
}

