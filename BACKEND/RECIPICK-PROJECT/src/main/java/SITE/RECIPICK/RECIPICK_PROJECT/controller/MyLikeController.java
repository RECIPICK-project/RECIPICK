package SITE.RECIPICK.RECIPICK_PROJECT.controller;

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
 * - 인증 연동 전에 임시로 사용자 id를 상수(ME)로 사용한다. - 페이징 파라미터(offset, limit)로 최신 좋아요 순서대로 PostDTO 목록을 반환한다. -
 * 정렬 기준은 서비스/레포지토리에서 '좋아요 시각(created_at)' 최근순으로 처리한다.
 * <p>
 * 주의 - 실제 운영 단계에서는 ME 상수를 제거하고, Security에서 사용자 식별자를 꺼내야 한다. - 401/403 등의 에러 응답은 추후
 *
 * @ControllerAdvice 또는 Security 예외 핸들러로 표준화 권장.
 */
@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
@Tag(name = "My Likes", description = "내가 좋아요한 레시피 조회 API")
public class MyLikeController {

  // 로그인 연동 전 임시 사용자 식별자 (추후 SecurityContext에서 꺼내도록 교체)
  private static final Integer ME = 1;

  private final MyLikeService myLikeService;

  /**
   * 내가 좋아요한 레시피 목록 조회
   *
   * @param offset 페이징 오프셋(0부터 시작)
   * @param limit  개수(기본 20, 과도한 값은 서비스에서 상한선 적용 권장)
   * @return PostDTO 리스트(최신 좋아요 순)
   */
  @GetMapping("/likes")
  @Operation(
      summary = "내가 좋아요한 레시피 조회",
      description = """
          내가 좋아요를 누른 레시피를 최신순으로 반환합니다.
          - 정렬 기준: '좋아요 생성 시각' 내림차순
          - 응답: PostDTO 배열
          - 인증 전 단계: ME=1 하드코딩
          """
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "성공",
          content = @Content(
              array = @ArraySchema(schema = @Schema(implementation = PostDTO.class))
          )
      ),
      @ApiResponse(
          responseCode = "401",
          description = "인증 실패(추후 보안 연동 시 적용)",
          content = @Content // 바디 없음
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 오류",
          content = @Content // 바디 없음(또는 에러 스키마 표준화 후 대체)
      )
  })
  public List<PostDTO> myLikes(
      @Parameter(description = "오프셋(0부터 시작)", example = "0")
      @RequestParam(defaultValue = "0") int offset,

      @Parameter(description = "가져올 개수(기본 20)", example = "20")
      @RequestParam(defaultValue = "20") int limit
  ) {
    // 서비스에 사용자 식별자와 페이징 파라미터를 전달.
    // 내부에서는 Repository가 like_table 기준으로 post를 조인, 최신 좋아요 순서로 뽑아온다.
    return myLikeService.getMyLikedPosts(ME, offset, limit);
  }
}
