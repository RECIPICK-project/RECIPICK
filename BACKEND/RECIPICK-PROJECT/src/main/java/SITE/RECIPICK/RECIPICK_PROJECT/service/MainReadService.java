package SITE.RECIPICK.RECIPICK_PROJECT.service;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.MainPostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.PostRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.WeatherRecommendRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MainReadService {

  private final PostRepository repo;
  private final KmaWeatherService kma;
  private final WeatherRecommendRepository weatherRepo;

  /** 오늘의 추천 (캐시된 날씨 기반 → 없으면 인기 fallback 5개, 네트워크 호출 없음) */
  public List<MainPostDto> todayTop5() {
    try {
      // ✅ 네트워크 호출 금지: 캐시에 있으면 그 조건, 없으면 "normal"
      String cond = kma.getCachedConditionOrNormal();
      log.debug("[MAIN] cond={}", cond);

      // cond별 추천
      List<MainPostDto> list = switch (cond) {
        case "rainy" -> mapDto(weatherRepo.rainyTop(5));
        case "hot"   -> mapDto(weatherRepo.hotTop(5));
        case "cold"  -> mapDto(weatherRepo.coldTop(5));
        case "snowy" -> mapDto(weatherRepo.snowyTop(5));
        default      -> List.of(); // normal이면 인기 보충으로
      };

      // normal이거나 결과가 비었으면, 캐시에 남아있는 최신 WeatherData로 온도 보정
      var w = kma.lastCachedWeather();
      if (list.isEmpty() && w != null) {
        Double tempC = w.temp();
        if (tempC != null) {
          if (tempC >= 24) {
            list = mapDto(weatherRepo.hotTop(5));
          } else if (tempC <= 12) {
            list = mapDto(weatherRepo.coldTop(5));
          }
        }
      }

      // 부족분은 인기에서 보충
      list = list.isEmpty() ? topNWithImage(5) : fillFromPopular(list, 5);

      log.debug("[MAIN] weather pick count={}", list.size());
      return list;

    } catch (Exception e) {
      log.warn("[MAIN] todayTop5 예외. fallback 수행", e);
      return topNWithImage(5);
    }
  }

  /** 인기 그리드: 좋아요/조회수/작성일 내림차순 → 상위 8 */
  public List<MainPostDto> popularTop8() {
    return topNWithImage(8);
  }

  // ===== 공통 =====

  private List<MainPostDto> topNWithImage(int n) {
    Sort sort = Sort.by(
        Sort.Order.desc("likeCount"),
        Sort.Order.desc("viewCount"),
        Sort.Order.desc("createdAt")
    );
    Pageable page = PageRequest.of(0, Math.max(30, n * 5), sort);

    return repo.findAll(page).getContent().stream()
        .filter(e -> e.getRcpImgUrl() != null && !e.getRcpImgUrl().isBlank())
        .sorted((a, b) -> {
          int cmp1 = Integer.compare(nvl(b.getLikeCount()), nvl(a.getLikeCount()));
          if (cmp1 != 0) return cmp1;
          int cmp2 = Integer.compare(nvl(b.getViewCount()), nvl(a.getViewCount()));
          if (cmp2 != 0) return cmp2;
          return nvl(b.getCreatedAt()).compareTo(nvl(a.getCreatedAt()));
        })
        .limit(n)
        .map(MainReadService::toMainDto)
        .toList();
  }

  private List<MainPostDto> mapDto(List<PostDto> src) {
    if (src == null) return List.of();
    return src.stream()
        .filter(d -> d.getRcpImgUrl() != null && !d.getRcpImgUrl().isBlank())
        .limit(5)
        .map(d -> new MainPostDto(
            d.getId(),                 // PostDto의 @JsonProperty("id")가 postId를 돌려줌
            safe(d.getTitle()),
            safe(d.getFoodName()),
            safe(d.getRcpImgUrl()),
            nvl(d.getLikeCount()),
            nvl(d.getViewCount())
        ))
        .toList();
  }

  private List<MainPostDto> fillFromPopular(List<MainPostDto> base, int target) {
    var rest = topNWithImage(target * 2); // 넉넉히 뽑아서 중복 제거
    Set<Integer> used = base.stream().map(MainPostDto::id).collect(Collectors.toSet());
    var add = rest.stream()
        .filter(p -> !used.contains(p.id()))
        .limit(target - base.size())
        .toList();
    return java.util.stream.Stream.concat(base.stream(), add.stream()).toList();
  }

  private static int nvl(Integer v) { return v == null ? 0 : v; }
  private static LocalDateTime nvl(LocalDateTime v) { return v == null ? LocalDateTime.MIN : v; }
  private static String safe(String s) { return s == null ? "" : s; }

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
}
