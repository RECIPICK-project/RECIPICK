package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.service.MyPostService;
import SITE.RECIPICK.RECIPICK_PROJECT.util.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 * 내 레시피 조회 컨트롤러
 * <p>
 * 특징 - 로그인된 사용자가 작성한 레시피를 조회 - type 파라미터로 "임시(temp)" / "정식(official)" 구분 (기본값: official) -
 * offset/limit 페이지네이션 지원
 * <p>
 * 인증 - 사용자 ID는 SecurityContext에서 CurrentUser 유틸을 통해 조회
 */
@RestController
@RequestMapping("/me/posts")
@RequiredArgsConstructor
@Tag(name = "My Posts (Queries)", description = "내 레시피 조회: 정식/임시 구분")
public class MyPostController {

  private final MyPostService myPostService;
  private final CurrentUser currentUser;

  /**
   * 내 레시피 조회 (정식/임시)
   *
   * @param type   조회할 레시피 종류 ("official" 또는 "temp")
   * @param offset 페이지 시작 위치(0부터)
   * @param limit  페이지 크기(기본 20)
   * @return PostDTO 리스트
   */
  @GetMapping
  @Operation(
      summary = "내 레시피 조회",
      description = """
          로그인 사용자가 작성한 레시피 목록을 조회합니다.
          - type=official → 내가 올린 정식 레시피
          - type=temp → 내가 올린 임시 레시피
          - 기본값은 official
          - 페이징: offset/limit 지원
          """
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증 필요"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  public List<PostDto> myPosts(
      @Parameter(description = "레시피 종류 (official/temp)", example = "official")
      @RequestParam(defaultValue = "official") String type,
      @Parameter(description = "페이지 시작 위치", example = "0")
      @RequestParam(defaultValue = "0") int offset,
      @Parameter(description = "페이지 크기", example = "20")
      @RequestParam(defaultValue = "20") int limit
  ) {
    // ✅ 인증된 사용자 ID 획득 (하드코딩 제거)
    Integer userId = currentUser.userId();

    // type이 temp면 임시 레시피, 아니면 정식 레시피 반환
    if ("temp".equalsIgnoreCase(type)) {
      return myPostService.getMyTempPosts(userId, offset, limit);
    }
    return myPostService.getMyOfficialPosts(userId, offset, limit);
  }
}
