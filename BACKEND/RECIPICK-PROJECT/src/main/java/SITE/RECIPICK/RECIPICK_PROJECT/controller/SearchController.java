package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class SearchController {

  @GetMapping("ingSearch")
  public String ingSearch() {
    return "ingSearch";
  }
}
