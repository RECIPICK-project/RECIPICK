package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import SITE.RECIPICK.RECIPICK_PROJECT.config.CurrentUserProvider;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.MyProfileResponse;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.NicknameUpdateRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostUpdateRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.ReviewDto;
import SITE.RECIPICK.RECIPICK_PROJECT.service.MyPageService;
import SITE.RECIPICK.RECIPICK_PROJECT.service.MyPostCommandService;
import SITE.RECIPICK.RECIPICK_PROJECT.util.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
@Tag(name = "My Page", description = "마이페이지: 프로필 조회/수정 APIs")
@RequiredArgsConstructor
public class MyPageController {

  private final MyPageService myPageService;
  private final CurrentUser currentUser;
  private final CurrentUserProvider currentUserProvider;
  private final MyPostCommandService svc;

  @GetMapping("/profile")
  @Operation(
      summary = "내 프로필 조회",
      description = """
          현재 사용자 프로필 정보를 반환합니다.
          - 닉네임, 등급, 프로필 이미지
          - 내가 올린 정식 레시피 개수
          - 내 정식 레시피의 총 좋아요 수
          - 활동 수(리뷰 + 댓글)
          """
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "성공",
          content = @Content(schema = @Schema(implementation = MyProfileResponse.class))),
      @ApiResponse(responseCode = "404", description = "프로필 없음(PROFILE_NOT_FOUND) — 테스트 계정 세팅 전일 수 있음"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  public MyProfileResponse getProfile() {
    return myPageService.getMyProfile(currentUser.userId());
  }

  @PatchMapping(value = "/profile/nickname", consumes = "application/json")
  @Operation(
      summary = "닉네임 변경(7일 제한)",
      description = """
          닉네임을 변경합니다.
          - 7일 쿨다운 적용(updated_at 기준)
          - 중복 닉네임 방지
          - 성공 시 본문 없는 204(No Content)
          """
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "변경 성공"),
      @ApiResponse(responseCode = "400", description = "요청 값 오류"),
      @ApiResponse(responseCode = "409", description = "쿨다운 또는 중복"),
      @ApiResponse(responseCode = "404", description = "프로필 없음"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  public ResponseEntity<?> changeNickname(@RequestBody NicknameUpdateRequest req) {
    try {
      myPageService.changeNickname(currentUser.userId(), req);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest()
          .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
          .body(e.getMessage() == null ? "BAD_REQUEST" : e.getMessage());
    } catch (IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
          .body(e.getMessage() == null ? "CONFLICT" : e.getMessage());
    }
  }

  @GetMapping("/likes")
  @Operation(
      summary = "내가 좋아요한 레시피 조회",
      description = """
          내가 좋아요를 누른 레시피를 최신순으로 반환합니다.
          - 정렬 기준: '좋아요 생성 시각' 내림차순
          - 응답: PostDTO 배열
          - 인증 필요
          """
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "성공",
          content = @Content(array = @ArraySchema(schema = @Schema(implementation = PostDto.class)))),
      @ApiResponse(responseCode = "401", description = "인증 실패"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  public List<PostDto> myLikes(
      @Parameter(description = "오프셋(0부터 시작)", example = "0")
      @RequestParam(defaultValue = "0") int offset,
      @Parameter(description = "가져올 개수(기본 20)", example = "20")
      @RequestParam(defaultValue = "20") int limit
  ) {
    Integer me = currentUserProvider.getCurrentUserId();
    return myPageService.getMyLikedPosts(me, offset, limit);
  }

  // (서비스에 있던 잘못된 매핑을 이쪽으로 이동)
  @GetMapping("/saved")
  @Operation(summary = "저장한 레시피 조회(= 좋아요 목록과 동일 정책)")
  public List<PostDto> mySaved(
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit
  ) {
    Integer me = currentUserProvider.getCurrentUserId();
    return myPageService.getMyLikedPosts(me, offset, limit);
  }

  @PatchMapping("/{postId}")
  @Operation(
      summary = "임시 레시피 수정",
      description = """
          본인이 작성한 임시 레시피를 수정합니다.
          - 정식 레시피는 수정 불가
          - 부분 수정 지원
          - 성공 시 최신 DTO 반환
          """
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "수정 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청"),
      @ApiResponse(responseCode = "403", description = "권한 없음"),
      @ApiResponse(responseCode = "404", description = "게시글 없음"),
      @ApiResponse(responseCode = "409", description = "정식 레시피 수정 시도"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  public PostDto updateTemp(
      @Parameter(description = "게시글 ID", required = true) @PathVariable Integer postId,
      @RequestBody PostUpdateRequest req
  ) {
    Integer userId = currentUser.userId();
    return svc.updateMyTempPost(userId, postId, req);
  }

  @DeleteMapping("/{postId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "임시 레시피 삭제",
      description = """
          본인이 작성한 임시 레시피를 삭제합니다.
          - 정식 레시피는 삭제 불가
          - 성공 시 204(No Content)
          """
  )
  public void deleteTemp(@PathVariable Integer postId) {
    Integer userId = currentUser.userId();
    svc.deleteMyTempPost(userId, postId);
  }

  @GetMapping("/reviews")
  @Operation(
      summary = "내가 작성한 리뷰 목록 조회",
      description = """
          현재 로그인한 사용자가 작성한 모든 리뷰를 최신순으로 조회합니다.
          마이페이지의 '내 리뷰' 탭에서 사용됩니다.
          """
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공",
          content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReviewDto.class)))),
      @ApiResponse(responseCode = "401", description = "인증 필요"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  public List<ReviewDto> getMyReviews(
      @Parameter(description = "오프셋(0부터 시작)", example = "0")
      @RequestParam(defaultValue = "0") int offset,
      @Parameter(description = "가져올 개수(기본 20)", example = "20")
      @RequestParam(defaultValue = "20") int limit
  ) {
    Integer userId = currentUser.userId();
    return myPageService.getMyReviews(userId, offset, limit);
  }
}
