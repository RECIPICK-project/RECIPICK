package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDTO;
import SITE.RECIPICK.RECIPICK_PROJECT.service.MyPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내 레시피 조회 컨트롤러
 * <p>
 * 특징: - 본인 작성 레시피를 조회 - type 파라미터로 "임시(temp)" / "정식(official)" 구분 - 기본값은 "official" - offset/limit
 * 페이징 지원
 */
@RestController
@RequestMapping("/me/posts")
@Tag(name = "My Posts (Queries)", description = "내 레시피 조회: 정식/임시 구분")
public class MyPostController {

  // 로그인 연동 전: userId = 1로 임시 고정
  private static final Integer ME = 1;

  private final MyPostService myPostService;

  public MyPostController(MyPostService myPostService) {
    this.myPostService = myPostService;
  }

  /**
   * 내 레시피 조회 (정식/임시)
   *
   * @param type   조회할 레시피 종류 ("official" 또는 "temp")
   * @param offset 페이지 시작 위치
   * @param limit  페이지 크기
   * @return PostDTO 리스트
   */
  @GetMapping
  @Operation(
      summary = "내 레시피 조회",
      description = """
          본인 작성 레시피 목록을 조회합니다.
          - type=official → 내가 올린 정식 레시피
          - type=temp → 내가 올린 임시 레시피
          - 기본값은 official
          - 페이징: offset/limit 지원
          """
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  public List<PostDTO> myPosts(
      @Parameter(description = "레시피 종류 (official/temp)", example = "official")
      @RequestParam(defaultValue = "official") String type,
      @Parameter(description = "페이지 시작 위치", example = "0")
      @RequestParam(defaultValue = "0") int offset,
      @Parameter(description = "페이지 크기", example = "20")
      @RequestParam(defaultValue = "20") int limit
  ) {
    // type이 temp일 경우 임시 레시피 조회
    if ("temp".equalsIgnoreCase(type)) {
      return myPostService.getMyTempPosts(ME, offset, limit);
    }
    // 나머지는 정식 레시피 조회
    return myPostService.getMyOfficialPosts(ME, offset, limit);
  }
}
