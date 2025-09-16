package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.service.PostService;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/post")
@CrossOrigin(origins = "*")
@Tag(name = "레시피 관리", description = "레시피 생성, 수정, 삭제 등을 관리하는 API")
public class PostRestController {

    private final PostService postService;

    /** 레시피 저장 API POST/post/save */
    @Operation(summary = "레시피 저장", description = "새로운 레시피를 저장합니다. 이미지 파일과 함께 레시피 정보를 등록할 수 있습니다.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "201",
                        description = "레시피가 성공적으로 저장됨",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(implementation = Map.class),
                                        examples =
                                                @ExampleObject(
                                                        name = "성공 응답 예시",
                                                        value =
                                                                """
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
                      """))),
                @ApiResponse(
                        responseCode = "400",
                        description = "잘못된 요청 데이터",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(implementation = Map.class),
                                        examples =
                                                @ExampleObject(
                                                        name = "오류 응답 예시",
                                                        value =
                                                                """
                      {
                        "success": false,
                        "message": "잘못된 요청입니다: 제목은 필수 입력값입니다."
                      }
                      """))),
                @ApiResponse(
                        responseCode = "500",
                        description = "서버 내부 오류",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(implementation = Map.class),
                                        examples =
                                                @ExampleObject(
                                                        name = "서버 오류 응답 예시",
                                                        value =
                                                                """
                      {
                        "success": false,
                        "message": "레시피 저장 중 서버 오류가 발생했습니다."
                      }
                      """)))
            })
    @PostMapping(value = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> saveRecipe(
            @Parameter(description = "저장할 레시피 정보", required = true) @Valid @ModelAttribute
                    PostDto postDto) {
        try {
            log.info("레시피 저장 요청 - 제목: {}, 음식명: {}", postDto.getTitle(), postDto.getFoodName());

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

    /** 전체 레시피 조회 API GET /post/all */
    @Operation(summary = "전체 레시피 조회", description = "전체 레시피 목록을 페이징하여 조회합니다. 메인페이지나 레시피 검색에서 사용됩니다.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "레시피 목록 조회 성공",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(implementation = Map.class),
                                        examples =
                                                @ExampleObject(
                                                        name = "성공 응답 예시",
                                                        value =
                                                                """
                      {
                        "success": true,
                        "data": {
                          "recipes": [
                            {
                              "title": "김치찌개",
                              "foodName": "김치찌개",
                              "ckgMth": "끓이기",
                              "ckgCategory": "채소류",
                              "ckgKnd": "찌개",
                              "ckgInbun": 2,
                              "ckgLevel": 2,
                              "ckgTime": 30,
                              "rcpImgUrl": "/images/recipe_1.jpg"
                            }
                          ],
                          "totalElements": 150,
                          "totalPages": 8,
                          "currentPage": 0,
                          "hasNext": true,
                          "hasPrevious": false
                        }
                      }
                      """))),
                @ApiResponse(
                        responseCode = "400",
                        description = "잘못된 요청 파라미터",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(implementation = Map.class),
                                        examples =
                                                @ExampleObject(
                                                        name = "오류 응답 예시",
                                                        value =
                                                                """
                      {
                        "success": false,
                        "message": "잘못된 페이지 번호입니다."
                      }
                      """))),
                @ApiResponse(
                        responseCode = "500",
                        description = "서버 내부 오류",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(implementation = Map.class),
                                        examples =
                                                @ExampleObject(
                                                        name = "서버 오류 응답 예시",
                                                        value =
                                                                """
                      {
                        "success": false,
                        "message": "레시피 조회 중 서버 오류가 발생했습니다."
                      }
                      """)))
            })
    @GetMapping("/all")
    public ResponseEntity<?> getAllRecipes(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0")
                    int page,
            @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20")
                    int size,
            @Parameter(description = "정렬 기준 (createdAt, likeCount, viewCount)", example = "createdAt")
                    @RequestParam(defaultValue = "createdAt")
                    String sortBy,
            @Parameter(description = "정렬 방향 (ASC, DESC)", example = "DESC")
                    @RequestParam(defaultValue = "DESC")
                    String sortDirection,
            @Parameter(description = "정식 레시피 여부 (1: 정식, 0: 임시)", example = "1")
                    @RequestParam(defaultValue = "1")
                    Integer official,
            @Parameter(description = "카테고리 필터", example = "채소류") @RequestParam(required = false)
                    String category,
            @Parameter(description = "조리방법 필터", example = "끓이기") @RequestParam(required = false)
                    String method) {

        try {
            log.info("전체 레시피 조회 요청 - page: {}, size: {}, sortBy: {}, direction: {}, official: {}, category: {}, method: {}",
                page, size, sortBy, sortDirection, official, category, method);

            // 파라미터 유효성 검사
            if (page < 0) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "페이지 번호는 0 이상이어야 합니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            if (size <= 0 || size > 100) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "페이지 크기는 1-100 사이여야 합니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // 레시피 조회
            Map<String, Object> recipesData = postService.getAllRecipes(page, size, sortBy, sortDirection, official, category, method);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", recipesData);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("전체 레시피 조회 실패 - 잘못된 요청: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "잘못된 요청입니다: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            log.error("전체 레시피 조회 실패 - 서버 오류: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "레시피 조회 중 서버 오류가 발생했습니다.");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /** 인기 레시피 조회 API GET /post/popular */
    @Operation(summary = "인기 레시피 조회", description = "좋아요 수가 많은 인기 레시피를 조회합니다.")
    @GetMapping("/popular")
    public ResponseEntity<?> getPopularRecipes(
            @Parameter(description = "조회할 레시피 개수", example = "10") @RequestParam(defaultValue = "10")
                    int limit) {

        try {
            log.info("인기 레시피 조회 요청 - limit: {}", limit);

            if (limit <= 0 || limit > 50) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "limit은 1-50 사이여야 합니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            List<PostDto> popularRecipes = postService.getPopularRecipes(limit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", popularRecipes);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("인기 레시피 조회 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "인기 레시피 조회 중 오류가 발생했습니다.");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /** 최신 레시피 조회 API GET /post/latest */
    @Operation(summary = "최신 레시피 조회", description = "최근에 등록된 레시피를 조회합니다.")
    @GetMapping("/latest")
    public ResponseEntity<?> getLatestRecipes(
            @Parameter(description = "조회할 레시피 개수", example = "10") @RequestParam(defaultValue = "10")
                    int limit) {

        try {
            log.info("최신 레시피 조회 요청 - limit: {}", limit);

            if (limit <= 0 || limit > 50) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "limit은 1-50 사이여야 합니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            List<PostDto> latestRecipes = postService.getLatestRecipes(limit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", latestRecipes);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("최신 레시피 조회 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "최신 레시피 조회 중 오류가 발생했습니다.");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /** 개별 레시피 상세 조회 API GET /post/{postId} */
    @Operation(summary = "레시피 상세 조회", description = "특정 레시피의 상세 정보를 조회합니다. 조회 시 조회수가 1 증가합니다.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "레시피 상세 조회 성공",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(implementation = Map.class),
                                        examples =
                                                @ExampleObject(
                                                        name = "성공 응답 예시",
                                                        value =
                                                                """
                      {
                        "success": true,
                        "data": {
                          "postId": 1,
                          "title": "소고기콩나물솥밥",
                          "author": "맛있는요리사",
                          "difficulty": 4,
                          "servings": 4,
                          "cookingTime": 45,
                          "cookingTimeString": "45 min",
                          "rcpImgUrl": "/images/recipe_1.jpg",
                          "ingredientsString": "쌀|4컵|콩나물|200g|소고기다짐육|100g",
                          "rcpSteps": ["쌀을 깨끗하게 씻어주세요", "소고기를 볶아주세요"],
                          "viewCount": 125,
                          "likeCount": 23,
                          "createdAt": "2024-03-15T10:30:00"
                        }
                      }
                      """))),
                @ApiResponse(
                        responseCode = "404",
                        description = "레시피를 찾을 수 없음",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(implementation = Map.class),
                                        examples =
                                                @ExampleObject(
                                                        name = "오류 응답 예시",
                                                        value =
                                                                """
                      {
                        "success": false,
                        "message": "해당 레시피를 찾을 수 없습니다."
                      }
                      """))),
                @ApiResponse(
                        responseCode = "500",
                        description = "서버 내부 오류",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(implementation = Map.class),
                                        examples =
                                                @ExampleObject(
                                                        name = "서버 오류 응답 예시",
                                                        value =
                                                                """
                      {
                        "success": false,
                        "message": "레시피 조회 중 서버 오류가 발생했습니다."
                      }
                      """)))
            })
    @GetMapping("/{postId}")
    public ResponseEntity<?> getRecipeById(
            @Parameter(description = "조회할 레시피 ID", example = "1", required = true) @PathVariable Integer postId) {

        try {
            log.info("레시피 상세 조회 요청 - postId: {}", postId);

            if (postId == null || postId <= 0) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "유효하지 않은 레시피 ID입니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            PostDto recipe = postService.getRecipeById(postId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", recipe);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("레시피 상세 조회 실패 - 레시피 없음: postId={}, message={}", postId, e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

        } catch (Exception e) {
            log.error("레시피 상세 조회 실패 - 서버 오류: postId={}, error={}", postId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "레시피 조회 중 서버 오류가 발생했습니다.");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
