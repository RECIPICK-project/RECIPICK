package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.service.PostService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

  private final PostService postService;

  @GetMapping("/search-test")
  public List<PostDto> searchTest(
      @RequestParam List<String> main,
      @RequestParam(required = false) List<String> sub,
      @RequestParam(defaultValue = "latest") String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    if (sub == null) {
      sub = List.of();
    }
    Pageable pageable = PageRequest.of(page, size);
    return postService.searchRecipes(main, sub, sort, pageable);
  }

  // 재료 자동완성 기능 추가
  @GetMapping("/ingredients/autocomplete")
  public List<String> autocompleteIngredients(
      @RequestParam String keyword,
      @RequestParam(defaultValue = "10") int limit
  ) {
    return postService.searchIngredients(keyword, limit);
  }
}