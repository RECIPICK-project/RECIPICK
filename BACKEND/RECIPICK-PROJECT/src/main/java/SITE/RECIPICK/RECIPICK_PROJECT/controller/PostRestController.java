package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/post")
@Tag(name = "레시피 관리", description = "레시피 생성, 수정, 삭제 등을 관리하는 API")
public class PostRestController {

  private final PostService postService;

  /**
   * 레시피 저장 API POST /post/save
   */
  @Operation(
      summary = "레시피 저장",
      description = "새로운 레시피를 저장합니다. 이미지 파일과 함께 레시피 정보를 등록할 수 있습니다."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201",
          description = "레시피가 성공적으로 저장됨",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Map.class),
              examples = @ExampleObject(
                  name = "성공 응답 예시",
                  value = """
                      {
                        "success": true,
                        "message": "레시피가 성공적으로 저장되었습니다.",
                        "data": {
                          "id": 1,
                          "title": "김치찌개",
                          "foodName": "김치찌개",
                          "content": "맛있는 김치찌개 레시피입니다.",
                          "cookingTime": 30,
                          "servings": 2,
                          "difficulty": "EASY",
                          "imageUrl": "/images/recipe_1.jpg"
                        }
                      }
                      """
              )
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = "잘못된 요청 데이터",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Map.class),
              examples = @ExampleObject(
                  name = "오류 응답 예시",
                  value = """
                      {
                        "success": false,
                        "message": "잘못된 요청입니다: 제목은 필수 입력값입니다."
                      }
                      """
              )
          )
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Map.class),
              examples = @ExampleObject(
                  name = "서버 오류 응답 예시",
                  value = """
                      {
                        "success": false,
                        "message": "레시피 저장 중 서버 오류가 발생했습니다."
                      }
                      """
              )
          )
      )
  })
  @PostMapping(value = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> saveRecipe(
      @Parameter(description = "저장할 레시피 정보", required = true)
      @Valid @ModelAttribute PostDto postDto) {
    try {
      log.info("레시피 저장 요청 - 제목: {}, 음식명: {}",
          postDto.getTitle(), postDto.getFoodName());

      PostDto savedRecipe = postService.saveRecipe(postDto);

      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("message", "레시피가 성공적으로 저장되었습니다.");
      response.put("data", savedRecipe);

      return ResponseEntity.status(HttpStatus.CREATED).body(response);

    } catch (IllegalArgumentException e) {
      log.warn("레시피 저장 실패 - 잘못된 요청: {}", e.getMessage());

      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("success", false);
      errorResponse.put("message", "잘못된 요청입니다: " + e.getMessage());

      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

    } catch (Exception e) {
      log.error("레시피 저장 실패 - 서버 오류: {}", e.getMessage(), e);

      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("success", false);
      errorResponse.put("message", "레시피 저장 중 서버 오류가 발생했습니다.");

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
  }
}