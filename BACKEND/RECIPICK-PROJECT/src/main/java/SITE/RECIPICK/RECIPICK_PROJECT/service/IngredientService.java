package SITE.RECIPICK.RECIPICK_PROJECT.service;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.IngredientResponseDto;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.Ingredient;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.IngredientRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IngredientService {

  private final IngredientRepository ingredientRepository;

  // 자동완성 검색
  public List<IngredientResponseDto> autocomplete(String keyword) {
    List<Ingredient> ingredients = ingredientRepository.findTop10ByNameContainingIgnoreCase(
        keyword);
    return ingredients.stream()
        .map(ing -> new IngredientResponseDto(ing.getIngId(), ing.getName(), ing.getSort()))
        .collect(Collectors.toList());
  }
}
