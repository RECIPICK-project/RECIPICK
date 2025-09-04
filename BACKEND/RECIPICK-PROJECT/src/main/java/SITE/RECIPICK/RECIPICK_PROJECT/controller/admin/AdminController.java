package SITE.RECIPICK.RECIPICK_PROJECT.controller.admin;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.GradeUpdateRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDTO;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.ReportCreateRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.ReportModerateRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.admin.AdminDashboardResponse;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.admin.UpdateUserActiveRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.admin.UserSummaryDTO;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.ReportEntity;
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
 * ✅ 관리자 전용 API 모음 - /admin 경로 하위에서만 동작 - Swagger @Tag 로 그룹화 - AdminService, AdminGradeService 와 연결
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "admin", description = "관리자 전용 API")
public class AdminController {

  private static final Integer ME = 1; // TODO: Security 붙이면 인증 사용자 ID로 교체 예정
  private final AdminService svc;      // 주요 관리자 기능 (게시글/신고/유저 관리)
  private final AdminGradeService gradeSvc; // 유저 등급 변경 전담 서비스

  // ================= Users =================

  /**
   * 유저 목록 조회 - 최신 가입 순서 - offset/limit 기반 페이지네이션
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
   * 유저 등급 변경 (BRONZE | SILVER | GOLD)
   */
  @PatchMapping(
      value = "/users/{userId}/grade",
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  @Operation(
      summary = "유저 등급 변경",
      description = "등급을 BRONZE | SILVER | GOLD 중 하나로 변경합니다."
  )
  public ResponseEntity<Void> updateUserGrade(
      @PathVariable Integer userId,
      @Valid @RequestBody GradeUpdateRequest req
  ) {
    gradeSvc.updateUserGrade(userId, req);
    return ResponseEntity.noContent().build(); // 204 No Content
  }

  /**
   * 유저 활성/정지 상태 변경 - active=true : 활성 - active=false : 정지
   */
  @PatchMapping("/users/{userId}/active")
  @Operation(summary = "유저 활성/정지", description = "active=true/false")
  public void updateUserActive(@PathVariable Integer userId,
      @RequestBody UpdateUserActiveRequest req) {
    svc.updateUserActive(userId, req.isActive());
  }

  // ================= Posts =================

  /**
   * 미승인(임시 저장) 레시피 목록 조회
   */
  @GetMapping("/posts/pending")
  @Operation(summary = "미승인(임시) 레시피 목록")
  public List<PostDTO> pendingPosts(
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
  public void publishPost(@PathVariable Long postId) {
    svc.publishPost(postId);
  }

  /**
   * 레시피 삭제
   */
  @DeleteMapping("/posts/{postId}")
  @Operation(summary = "레시피 삭제")
  public void deletePost(@PathVariable Long postId) {
    svc.deletePost(postId);
  }

  // ================= Reports =================

  /**
   * 신고 누적 상위 레시피 조회 - reportCount > min 조건
   */
  @GetMapping("/reports/posts")
  @Operation(summary = "신고 많은 레시피", description = "reportCount > min")
  public List<PostDTO> reportedPosts(
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "0") int min
  ) {
    return svc.topReportedPosts(min, offset, limit);
  }

  /**
   * 특정 리뷰 삭제 (관리자 권한)
   */
  @DeleteMapping("/reports/reviews/{reviewId}")
  @Operation(summary = "리뷰 삭제")
  public void deleteReview(@PathVariable Integer reviewId) {
    svc.deleteReview(reviewId);
  }

  /**
   * 특정 댓글 삭제 (관리자 권한)
   */
  @DeleteMapping("/reports/comments/{commentId}")
  @Operation(summary = "댓글 삭제")
  public void deleteComment(@PathVariable Integer commentId) {
    svc.deleteComment(commentId);
  }

  /**
   * 사용자 신고 등록 - 일반 사용자가 사용하는 신고 등록 API - 현재는 /admin/reports 로 둠 (추후 /reports 로 이동 가능)
   */
  @PostMapping("/reports")
  @Operation(summary = "신고 등록")
  public void createReport(@Valid @RequestBody ReportCreateRequest req) {
    svc.createReport(ME, req);
  }

  /**
   * 신고 목록 조회 - status : PENDING | ACCEPTED | REJECTED - type   : POST | REVIEW | COMMENT | USER
   */
  @GetMapping("/reports")
  @Operation(summary = "신고 목록 조회", description = "status=PENDING|ACCEPTED|REJECTED, type=POST|REVIEW|COMMENT|USER")
  public Page<ReportEntity> listReports(
      @RequestParam(defaultValue = "PENDING") String status,
      @RequestParam(required = false) String type,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    return svc.listReports(status, type, page, size);
  }

  /**
   * 신고 처리 (관리자 전용) - action=ACCEPT → ACCEPTED - action=REJECT → REJECTED
   */
  @PatchMapping("/reports/{id}")
  @Operation(summary = "신고 처리", description = "action=ACCEPT|REJECT")
  public void moderate(@PathVariable Long id, @RequestBody ReportModerateRequest req) {
    svc.moderate(id, req);
  }

  // ================= Dashboard =================

  /**
   * 관리자 대시보드 통계 조회 - 회원 수, 게시글 수, 신고 현황 등을 집계
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

}
