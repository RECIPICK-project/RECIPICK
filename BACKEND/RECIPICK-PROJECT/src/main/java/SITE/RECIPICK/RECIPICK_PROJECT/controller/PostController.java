package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostResponseDto;
import SITE.RECIPICK.RECIPICK_PROJECT.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

  private final PostService postService;

  // GET 방식으로 main/sub/sort/page/size 전달 가능
  @GetMapping("/search-test")
  public Page<PostResponseDto> searchTest(
      @RequestParam(required = false) String main,
      @RequestParam(required = false) String sub,
      @RequestParam(defaultValue = "latest") String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  ) {
    return postService.searchRecipes(main, sub, sort, page, size);
  }
}
