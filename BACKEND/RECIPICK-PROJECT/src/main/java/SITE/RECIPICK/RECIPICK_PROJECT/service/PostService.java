package SITE.RECIPICK.RECIPICK_PROJECT.service;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostResponseDto;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.Post;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService {

  private final PostRepository postRepository;

  // GET 방식 연동
  public Page<PostResponseDto> searchRecipes(String main, String sub, String sort, int page,
      int size) {
    Pageable pageable = PageRequest.of(page, size, getSort(sort));
    Page<Post> posts = postRepository.search(main, sub, pageable);
    return posts.map(PostResponseDto::fromEntity);
  }

  private Sort getSort(String sort) {
    if ("views".equalsIgnoreCase(sort)) {
      return Sort.by(Sort.Direction.DESC, "viewCount");
    } else if ("likes".equalsIgnoreCase(sort)) {
      return Sort.by(Sort.Direction.DESC, "likeCount");
    } else { // latest
      return Sort.by(Sort.Direction.DESC, "createdAt");
    }
  }
}
