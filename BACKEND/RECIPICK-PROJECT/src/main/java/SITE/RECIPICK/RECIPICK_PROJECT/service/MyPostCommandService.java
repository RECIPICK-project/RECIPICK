package SITE.RECIPICK.RECIPICK_PROJECT.service;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostUpdateRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.Ingredient;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity.CookingCategory;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity.CookingKind;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity.CookingMethod;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.RecipeIngredient;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.IngredientRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.PostRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.RecipeIngredientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static SITE.RECIPICK.RECIPICK_PROJECT.util.PostMapper.toDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class MyPostCommandService {

    private static final String JOINER = "|";
    private final PostRepository postRepo;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final IngredientRepository ingredientRepository;

    private static String join(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return list.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(JOINER));
    }

    private static CookingMethod parseCookingMethod(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        for (CookingMethod v : CookingMethod.values()) {
            if (v.name().equalsIgnoreCase(raw)) {
                return v;
            }
            if (v.getDescription().equalsIgnoreCase(raw)) {
                return v;
            }
        }
        throw new IllegalArgumentException("INVALID_COOKING_METHOD");
    }

    private static CookingCategory parseCookingCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        for (CookingCategory v : CookingCategory.values()) {
            if (v.name().equalsIgnoreCase(raw)) {
                return v;
            }
            if (v.getDescription().equalsIgnoreCase(raw)) {
                return v;
            }
        }
        throw new IllegalArgumentException("INVALID_COOKING_CATEGORY");
    }

    private static CookingKind parseCookingKind(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        for (CookingKind v : CookingKind.values()) {
            if (v.name().equalsIgnoreCase(raw)) {
                return v;
            }
            if (v.getDescription().equalsIgnoreCase(raw)) {
                return v;
            }
        }
        throw new IllegalArgumentException("INVALID_COOKING_KIND");
    }

    @Transactional
    public PostDto updateMyTempPost(Integer me, Integer postId, PostUpdateRequest req) {
        PostEntity p = postRepo.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("POST_NOT_FOUND"));

        if (!Objects.equals(p.getUserId(), me)) {
            throw new IllegalStateException("FORBIDDEN");
        }
        if (p.getRcpIsOfficial() != null && p.getRcpIsOfficial() == 1) {
            throw new IllegalStateException("ONLY_TEMP_EDITABLE");
        }

        // 기본 필드 업데이트
        if (req.getTitle() != null) p.setTitle(req.getTitle());
        if (req.getFoodName() != null) p.setFoodName(req.getFoodName());
        if (req.getCkgMth() != null) p.setCkgMth(parseCookingMethod(req.getCkgMth()));
        if (req.getCkgCategory() != null) p.setCkgCategory(parseCookingCategory(req.getCkgCategory()));
        if (req.getCkgKnd() != null) p.setCkgKnd(parseCookingKind(req.getCkgKnd()));
        if (req.getCkgInbun() != null) p.setCkgInbun(req.getCkgInbun());
        if (req.getCkgLevel() != null) p.setCkgLevel(req.getCkgLevel());
        if (req.getCkgTime() != null) p.setCkgTime(req.getCkgTime());
        if (req.getRcpImgUrl() != null) p.setRcpImgUrl(req.getRcpImgUrl());
        if (req.getRcpSteps() != null) p.setRcpSteps(join(req.getRcpSteps()));
        if (req.getRcpStepsImg() != null) p.setRcpStepsImg(join(req.getRcpStepsImg()));

        // ckg_mtrl_cn 필드 업데이트
        if (req.getCkgMtrlCn() != null && !req.getCkgMtrlCn().isEmpty()) {
            String formattedIngredients = "[재료] " + String.join(" | ", req.getCkgMtrlCn());
            p.setCkgMtrlCn(formattedIngredients);
        }

        // ===== 레시피-재료 연관관계 업데이트 =====
        if (req.getIngredientNames() != null && !req.getIngredientNames().isEmpty()) {

            // 1. 기존 레시피-재료 관계 모두 삭제
            List<RecipeIngredient> existingIngredients = recipeIngredientRepository.findByPostId(postId);
            if (!existingIngredients.isEmpty()) {
                recipeIngredientRepository.deleteByPostId(postId);
            }

            // 2. 새로운 재료 추가
            List<RecipeIngredient> newRecipeIngredients = new ArrayList<>();
            List<String> names = req.getIngredientNames();
            List<String> quantities = req.getIngredientQuantities();
            List<String> units = req.getIngredientUnits();

            for (int i = 0; i < names.size(); i++) {
                String name = names.get(i);
                if (name == null || name.isBlank()) continue;

                name = name.trim();

                // 재료 찾기 또는 생성
                List<Ingredient> existing = ingredientRepository.findByName(name);
                Ingredient ingredient;
                if (!existing.isEmpty()) {
                    ingredient = existing.get(0);
                } else {
                    Ingredient newIng = new Ingredient();
                    newIng.setName(name);
                    newIng.setSort(getIngredientCategoryByName(name));
                    ingredient = ingredientRepository.save(newIng);
                }

                String quantity = (quantities != null && i < quantities.size() && quantities.get(i) != null) ?
                        quantities.get(i).trim() : "";
                String unit = (units != null && i < units.size() && units.get(i) != null) ?
                        units.get(i).trim() : "";
                String amount = (quantity + unit).trim();

                RecipeIngredient recipeIngredient = RecipeIngredient.builder()
                        .postId(postId)
                        .ingId(ingredient.getIngId())
                        .amount(amount)
                        .build();
                newRecipeIngredients.add(recipeIngredient);
            }

            // 3. 일괄 저장
            if (!newRecipeIngredients.isEmpty()) {
                recipeIngredientRepository.saveAll(newRecipeIngredients);
            }
        }

        p.setUpdatedAt(LocalDateTime.now());
        PostEntity saved = postRepo.save(p);

        return toDto(saved);
    }

    @Transactional
    public void deleteMyTempPost(Integer me, Integer postId) {
        PostEntity p = postRepo.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("POST_NOT_FOUND"));

        if (!Objects.equals(p.getUserId(), me)) {
            throw new IllegalStateException("FORBIDDEN");
        }
        if (p.getRcpIsOfficial() != null && p.getRcpIsOfficial() == 1) {
            throw new IllegalStateException("ONLY_TEMP_DELETABLE");
        }

        // 연관된 재료 먼저 삭제
        recipeIngredientRepository.deleteByPostId(postId);

        // 레시피 삭제
        postRepo.delete(p);
    }

    @Transactional(readOnly = true)
    public PostDto getMyTempPost(Integer me, Integer postId) {
        PostEntity p = postRepo.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("POST_NOT_FOUND"));
        if (!Objects.equals(p.getUserId(), me)) {
            throw new IllegalStateException("FORBIDDEN");
        }
        if (p.getRcpIsOfficial() != null && p.getRcpIsOfficial() == 1) {
            throw new IllegalStateException("ONLY_TEMP_READABLE_ON_EDIT");
        }
        return toDto(p);
    }

    // 재료명에 따라 자동으로 카테고리 분류하는 메서드
    private String getIngredientCategoryByName(String ingredientName) {
        String name = ingredientName.toLowerCase().trim();

        if (name.contains("소고기") || name.contains("한우") || name.contains("갈비")) {
            return "소고기";
        }
        if (name.contains("돼지고기") || name.contains("삼겹살") || name.contains("목살")) {
            return "돼지고기";
        }
        if (name.contains("닭고기") || name.contains("닭") || name.contains("치킨")) {
            return "닭고기";
        }
        if (name.contains("쌀") || name.contains("백미") || name.contains("현미")) {
            return "쌀";
        }
        if (name.contains("밀가루") || name.contains("전분")) {
            return "밀가루";
        }
        if (name.contains("버섯")) {
            return "버섯류";
        }
        if (name.contains("생선") || name.contains("새우") || name.contains("오징어")) {
            return "해물류";
        }
        if (name.contains("양파") || name.contains("마늘") || name.contains("당근") || name.contains("감자")) {
            return "채소류";
        }
        if (name.contains("계란") || name.contains("달걀") || name.contains("우유") || name.contains("치즈")) {
            return "달걀/유제품";
        }
        if (name.contains("콩") || name.contains("두부") || name.contains("된장")) {
            return "콩/견과류";
        }
        if (name.contains("사과") || name.contains("배") || name.contains("바나나")) {
            return "과일류";
        }

        return "기타";
    }
}