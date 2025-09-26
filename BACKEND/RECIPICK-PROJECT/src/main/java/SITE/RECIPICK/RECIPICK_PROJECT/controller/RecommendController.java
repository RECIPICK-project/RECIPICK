package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.WeatherData;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.WeatherRecommendRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.service.KmaWeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommend")
public class RecommendController {

  private final KmaWeatherService kmaService;
  private final WeatherRecommendRepository repo;

  @GetMapping("/today")
  public Map<String,Object> today() {

    WeatherData data = kmaService.lastCachedWeather();   // null일 수 있음
    String cond = kmaService.getCachedConditionOrNormal();

    var items = switch(cond) {
      case "rainy" -> repo.rainyTop(6);
      case "snowy" -> repo.snowyTop(6);
      case "cold"  -> repo.coldTop(6);
      case "hot"   -> repo.hotTop(6);
      default      -> repo.hotTop(6);
    };

    return Map.of(
        "condition", cond,
        "weather", data,
        "items", items
    );
  }
}
