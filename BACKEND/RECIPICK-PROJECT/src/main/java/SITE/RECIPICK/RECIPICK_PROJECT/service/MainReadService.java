package SITE.RECIPICK.RECIPICK_PROJECT.service;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.MainPostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.PostRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MainReadService {

  private final PostRepository repo;

  /**
   * 오늘의 추천: 좋아요 → 조회수 → 생성일 내림차순, 이미지 있는 것만 상위 5
   */
  public List<MainPostDto> todayTop5() {
    return topNWithImage(5);
  }

  /**
   * 인기 그리드: 좋아요 → 조회수 → 생성일 내림차순, 이미지 있는 것만 상위 8
   */
  public List<MainPostDto> popularTop8() {
    return topNWithImage(8);
  }

  // ===== 내부 공통 로직 =====
  private List<MainPostDto> topNWithImage(int n) {
    // 정렬: likeCount DESC, viewCount DESC, createdAt DESC
    Sort sort = Sort.by(
        Sort.Order.desc("likeCount"),
        Sort.Order.desc("viewCount"),
        Sort.Order.desc("createdAt")
    );

    // 넉넉히 가져온 다음(예: 100개) 프리필터링
    Pageable page = PageRequest.of(0, Math.max(30, n * 5), sort);

    return repo.findAll(page).getContent().stream()
        // 이미지 있는 것만
        .filter(e -> e.getRcpImgUrl() != null && !e.getRcpImgUrl().isBlank())
        // (옵션) 정식 레시피만 보고 싶으면 주석 해제
        // .filter(e -> Objects.equals(e.getRcpIsOfficial(), 1))
        // 널 안전성 보정(혹시 like/view/createdAt이 널인 데이터가 있더라도 정렬 유지)
        .sorted((a, b) -> {
          int cmp1 = Integer.compare(nvl(b.getLikeCount()), nvl(a.getLikeCount()));
          if (cmp1 != 0) return cmp1;
          int cmp2 = Integer.compare(nvl(b.getViewCount()), nvl(a.getViewCount()));
          if (cmp2 != 0) return cmp2;
          return nvl(b.getCreatedAt()).compareTo(nvl(a.getCreatedAt()));
        })
        .limit(n)
        .map(MainReadService::toMainDto)
        .collect(Collectors.toList());
  }

  private static int nvl(Integer v) { return v == null ? 0 : v; }
  private static LocalDateTime nvl(LocalDateTime v) { return v == null ? LocalDateTime.MIN : v; }

  private static MainPostDto toMainDto(PostEntity e) {
    return new MainPostDto(
        e.getPostId(),
        safe(e.getTitle()),
        safe(e.getFoodName()),
        safe(e.getRcpImgUrl()),
        nvl(e.getLikeCount()),
        nvl(e.getViewCount())
    );
  }

  private static String safe(String s) { return s == null ? "" : s; }
}
