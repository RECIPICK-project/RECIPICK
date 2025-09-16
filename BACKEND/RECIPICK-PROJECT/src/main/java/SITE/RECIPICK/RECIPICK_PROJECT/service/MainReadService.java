package SITE.RECIPICK.RECIPICK_PROJECT.service;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.MainPostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.PostRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MainReadService {

  private final PostRepository repo;

  public List<MainPostDto> todayTop5() {
    return repo.findTop5ByRcpImgUrlIsNotNullOrderByLikeCountDescViewCountDescCreatedAtDesc()
        .stream().map(MainReadService::toMainDto).toList();
  }

  public List<MainPostDto> popularTop8() {
    return repo.findTop8ByRcpImgUrlIsNotNullOrderByLikeCountDescViewCountDescCreatedAtDesc()
        .stream().map(MainReadService::toMainDto).toList();
  }

  private static MainPostDto toMainDto(PostEntity e) {
    return new MainPostDto(
        e.getPostId(),
        e.getTitle(),
        e.getFoodName(),
        e.getRcpImgUrl(),
        e.getLikeCount(),
        e.getViewCount()
    );
  }
}