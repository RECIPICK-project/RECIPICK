package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.service.PostService;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/post")
public class PostController {

  private final PostService postService;

  /**
   * 레시피 저장 API POST /api/post/save
   */
  @PostMapping("/save")
  public ResponseEntity<?> saveRecipe(@Valid @ModelAttribute PostDto postDto) {
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