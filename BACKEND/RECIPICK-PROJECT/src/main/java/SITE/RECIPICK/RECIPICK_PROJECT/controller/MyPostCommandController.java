/*
package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostUpdateRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.service.MyPostCommandService;
import SITE.RECIPICK.RECIPICK_PROJECT.util.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/me/posts")
@Tag(name = "My Posts (Commands)", description = "내 레시피 관리: 임시 레시피 수정·삭제")
@RequiredArgsConstructor // ★ 생성자 주입
public class MyPostCommandController {

    private final MyPostCommandService svc;
    private final CurrentUser currentUser; // ★ 주입

    @PatchMapping("/{postId}")
    @Operation(
            summary = "임시 레시피 수정",
            description =
                    """
          본인이 작성한 임시 레시피를 수정합니다.
          - 정식 레시피는 수정 불가
          - 부분 수정 지원 (넘어온 필드만 업데이트)
          - 수정 성공 시 최신 상태 DTO 반환
          """)
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
            @RequestBody PostUpdateRequest req) {
        Integer userId = currentUser.userId(); // ★ 여기
        return svc.updateMyTempPost(userId, postId, req); // ★ 서비스 시그니처에 맞게
    }

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "임시 레시피 삭제",
            description =
                    """
          본인이 작성한 임시 레시피를 삭제합니다.
          - 정식 레시피는 삭제 불가
          - 성공 시 204(No Content) 반환
          """)
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "게시글 없음"),
        @ApiResponse(responseCode = "409", description = "정식 레시피 삭제 시도"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public void deleteTemp(
            @Parameter(description = "게시글 ID", required = true) @PathVariable Integer postId) {
        Integer userId = currentUser.userId(); // ★ 여기
        svc.deleteMyTempPost(userId, postId); // ★ 서비스 시그니처에 맞게
    }
}
*/
