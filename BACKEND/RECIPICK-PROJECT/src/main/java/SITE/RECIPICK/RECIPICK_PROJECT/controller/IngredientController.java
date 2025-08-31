package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.IngredientResponseDto;
import SITE.RECIPICK.RECIPICK_PROJECT.service.IngredientService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
public class IngredientController {

  private final IngredientService ingredientService;

  // GET /api/ingredients/autocomplete?keyword=삼겹살
  @GetMapping("/autocomplete")
  public List<IngredientResponseDto> autocomplete(@RequestParam String keyword) {
    return ingredientService.autocomplete(keyword);
  }
}
