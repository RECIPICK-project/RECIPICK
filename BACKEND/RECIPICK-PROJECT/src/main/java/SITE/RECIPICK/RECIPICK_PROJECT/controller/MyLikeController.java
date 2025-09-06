package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import SITE.RECIPICK.RECIPICK_PROJECT.config.CurrentUserProvider;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDTO;
import SITE.RECIPICK.RECIPICK_PROJECT.service.MyLikeService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내가 '좋아요'한 레시피 조회 컨트롤러
 * <p>
 * - 인증 사용자 기준으로 최신 좋아요 순서대로 반환 - 페이징: offset/limit - SecurityContext의 이메일(username)로 userId 조회
 */
@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
@Tag(name = "My Likes", description = "내가 좋아요한 레시피 조회 API")
public class MyLikeController {

  private final MyLikeService myLikeService;
  private final CurrentUserProvider currentUserProvider;

  @GetMapping("/likes")
  @Operation(
      summary = "내가 좋아요한 레시피 조회",
      description = """
          내가 좋아요를 누른 레시피를 최신순으로 반환합니다.
          - 정렬 기준: '좋아요 생성 시각' 내림차순
          - 응답: PostDTO 배열
          - 인증 필요: httpBasic / 세션 등
          """
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "성공",
          content = @Content(array = @ArraySchema(schema = @Schema(implementation = PostDTO.class)))
      ),
      @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
      @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
  })
  public List<PostDTO> myLikes(
      @Parameter(description = "오프셋(0부터 시작)", example = "0")
      @RequestParam(defaultValue = "0") int offset,
      @Parameter(description = "가져올 개수(기본 20)", example = "20")
      @RequestParam(defaultValue = "20") int limit
  ) {
    Integer me = currentUserProvider.getCurrentUserId();
    return myLikeService.getMyLikedPosts(me, offset, limit);
  }
}
