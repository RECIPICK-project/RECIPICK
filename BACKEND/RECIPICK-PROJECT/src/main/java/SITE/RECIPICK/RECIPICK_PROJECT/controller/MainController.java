package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.MainPostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.service.MainReadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Main", description = "메인 섹션 API")
@RestController
@RequestMapping("/api/main")
@RequiredArgsConstructor
public class MainController {

  private final MainReadService svc;

  @Operation(summary = "오늘의 추천(좋아요 상위 5)")
  @GetMapping("/today")
  public List<MainPostDto> today() {
    return svc.todayTop5();
  }

  @Operation(summary = "인기 레시피(좋아요 상위 8)")
  @GetMapping("/popular-grid")
  public List<MainPostDto> popularGrid() {
    return svc.popularTop8();
  }
}