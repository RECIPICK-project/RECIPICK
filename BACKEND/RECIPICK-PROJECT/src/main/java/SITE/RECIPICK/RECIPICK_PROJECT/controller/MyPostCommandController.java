package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import static SITE.RECIPICK.RECIPICK_PROJECT.util.CurrentUser.currentUserId;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDTO;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostUpdateRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.service.MyPostCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내 레시피 관리 - 수정/삭제 전용 컨트롤러
 * <p>
 * 특징 - 본인 작성 "임시 레시피"만 수정/삭제 가능 - 정식 레시피(rcp_is_official=1)는 수정/삭제 불가 (서비스 계층에서 검증) - 실제 DB 업데이트는
 * JPA Dirty Checking으로 처리
 * <p>
 * 인증/권한 - userId는 SecurityContext에서 꺼냄 → util.CurrentUser.currentUserId() - SecurityConfig에서 /me/**
 * 는 인증 필요하게 설정되어 있어야 함
 */
@RestController
@RequestMapping("/me/posts")
@Tag(name = "My Posts (Commands)", description = "내 레시피 관리: 임시 레시피 수정·삭제")
public class MyPostCommandController {

  private final MyPostCommandService svc;

  public MyPostCommandController(MyPostCommandService svc) {
    this.svc = svc;
  }

  /**
   * 임시 레시피 수정
   *
   * @param postId 수정할 게시글 ID
   * @param req    수정할 필드들(title, foodName, 재료 등 — 부분 수정 허용)
   * @return 수정 완료된 PostDTO
   */
  @PatchMapping("/{postId}")
  @Operation(
      summary = "임시 레시피 수정",
      description = """
          본인이 작성한 임시 레시피를 수정합니다.
          - 정식 레시피는 수정 불가
          - 부분 수정 지원 (넘어온 필드만 업데이트)
          - 수정 성공 시 최신 상태 DTO 반환
          """
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "수정 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (필드 검증 실패 등)"),
      @ApiResponse(responseCode = "403", description = "권한 없음 (다른 사람 게시글 시도)"),
      @ApiResponse(responseCode = "404", description = "게시글 없음"),
      @ApiResponse(responseCode = "409", description = "정식 레시피 수정 시도"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  public PostDTO updateTemp(
      @Parameter(description = "게시글 ID", required = true) @PathVariable Long postId,
      @RequestBody PostUpdateRequest req
  ) {
    return svc.updateMyTempPost(currentUserId(), postId, req);
  }

  /**
   * 임시 레시피 삭제
   *
   * @param postId 삭제할 게시글 ID
   */
  @DeleteMapping("/{postId}")
  @Operation(
      summary = "임시 레시피 삭제",
      description = """
          본인이 작성한 임시 레시피를 삭제합니다.
          - 정식 레시피는 삭제 불가
          - 성공 시 204(No Content) 반환
          """
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "삭제 성공"),
      @ApiResponse(responseCode = "403", description = "권한 없음 (다른 사람 게시글 시도)"),
      @ApiResponse(responseCode = "404", description = "게시글 없음"),
      @ApiResponse(responseCode = "409", description = "정식 레시피 삭제 시도"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  public void deleteTemp(
      @Parameter(description = "게시글 ID", required = true) @PathVariable Long postId
  ) {
    svc.deleteMyTempPost(currentUserId(), postId);
  }
}
