package SITE.RECIPICK.RECIPICK_PROJECT.util;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity.CookingCategory;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity.CookingKind;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity.CookingMethod;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class PostMapper {

  private static final String SPLIT = "\\|";
  private static final String JOIN = "|";

  private PostMapper() {
  }

  /* ========== Entity -> DTO (응답) ========== */
  public static PostDto toDto(PostEntity p) {
    if (p == null) {
      return null;
    }

    return PostDto.builder()
        .title(p.getTitle())
        .foodName(p.getFoodName())

        // enum -> String (설명 한글 라벨로 내려줌; 필요하면 .name()으로 교체)
        .ckgMth(p.getCkgMth() == null ? null : p.getCkgMth().getDescription())
        .ckgCategory(p.getCkgCategory() == null ? null : p.getCkgCategory().getDescription())
        .ckgKnd(p.getCkgKnd() == null ? null : p.getCkgKnd().getDescription())

        // '|' 문자열 -> List<String>
        .ckgMtrlCn(split(p.getCkgMtrlCn()))

        .ckgInbun(p.getCkgInbun())
        .ckgLevel(p.getCkgLevel())
        .ckgTime(p.getCkgTime())

        .rcpImgUrl(p.getRcpImgUrl())
        .rcpSteps(split(p.getRcpSteps()))
        .rcpStepsImg(split(p.getRcpStepsImg()))
        .build();
  }

  /* ========== DTO -> Entity (등록/수정 입력) ========== */
  public static PostEntity toEntity(PostDto d) {
    if (d == null) {
      return null;
    }

    PostEntity e = new PostEntity();
    // PK/userId/집계/플래그/시간은 여기서 세팅하지 않음 (서비스/라이프사이클에서 관리)

    e.setTitle(d.getTitle());
    e.setFoodName(d.getFoodName());

    // String -> Enum (대소문자 무시 + 한글 라벨 허용)
    e.setCkgMth(parseCookingMethod(d.getCkgMth()));
    e.setCkgCategory(parseCookingCategory(d.getCkgCategory()));
    e.setCkgKnd(parseCookingKind(d.getCkgKnd()));

    // List<String> -> '|' 조인
    e.setCkgMtrlCn(join(d.getCkgMtrlCn()));

    e.setCkgInbun(d.getCkgInbun());
    e.setCkgLevel(d.getCkgLevel());
    e.setCkgTime(d.getCkgTime());

    e.setRcpImgUrl(d.getRcpImgUrl());
    e.setRcpSteps(join(d.getRcpSteps()));
    e.setRcpStepsImg(join(d.getRcpStepsImg()));

    return e;
  }

  /* ========== helpers ========== */
  private static List<String> split(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    return new ArrayList<>(Arrays.asList(raw.split(SPLIT, -1)));
  }

  private static String join(List<String> list) {
    if (list == null || list.isEmpty()) {
      return "";
    }
    return String.join(JOIN, list.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList());
  }

  private static CookingMethod parseCookingMethod(String s) {
    if (s == null || s.isBlank()) {
      return null;
    }
    for (CookingMethod v : CookingMethod.values()) {
      if (v.name().equalsIgnoreCase(s)) {
        return v;               // 영문 상수명
      }
      if (v.getDescription().equalsIgnoreCase(s)) {
        return v;     // 한글 라벨
      }
    }
    throw new IllegalArgumentException("INVALID_COOKING_METHOD");
  }

  private static CookingCategory parseCookingCategory(String s) {
    if (s == null || s.isBlank()) {
      return null;
    }
    for (CookingCategory v : CookingCategory.values()) {
      if (v.name().equalsIgnoreCase(s)) {
        return v;
      }
      if (v.getDescription().equalsIgnoreCase(s)) {
        return v;
      }
    }
    throw new IllegalArgumentException("INVALID_COOKING_CATEGORY");
  }

  private static CookingKind parseCookingKind(String s) {
    if (s == null || s.isBlank()) {
      return null;
    }
    for (CookingKind v : CookingKind.values()) {
      if (v.name().equalsIgnoreCase(s)) {
        return v;
      }
      if (v.getDescription().equalsIgnoreCase(s)) {
        return v;
      }
    }
    throw new IllegalArgumentException("INVALID_COOKING_KIND");
  }
}
