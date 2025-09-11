package SITE.RECIPICK.RECIPICK_PROJECT.service;

import static SITE.RECIPICK.RECIPICK_PROJECT.util.PostMapper.toDto;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostUpdateRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity.CookingCategory;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity.CookingKind;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity.CookingMethod;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.PostRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyPostCommandService {

  private static final String JOINER = "|";
  private final PostRepository postRepo;

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
        return v;              // enum name
      }
      if (v.getDescription().equalsIgnoreCase(raw)) {
        return v;    // 한글 라벨
      }
    }
    throw new IllegalArgumentException("INVALID_COOKING_METHOD");
  }

  /* ---------- helpers ---------- */

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

    // 2) 소유자 체크 (엔티티는 userId Integer)
    if (!Objects.equals(p.getUserId(), me)) {
      throw new IllegalStateException("FORBIDDEN");
    }

    // 3) 임시글만 수정 가능 (rcpIsOfficial: 0/1 플래그)
    if (p.getRcpIsOfficial() != null && p.getRcpIsOfficial() == 1) {
      throw new IllegalStateException("ONLY_TEMP_EDITABLE");
    }

    // 4) 부분 수정
    if (req.getTitle() != null) {
      p.setTitle(req.getTitle());
    }
    if (req.getFoodName() != null) {
      p.setFoodName(req.getFoodName());
    }

    if (req.getCkgMth() != null) {
      p.setCkgMth(parseCookingMethod(req.getCkgMth()));
    }
    if (req.getCkgCategory() != null) {
      p.setCkgCategory(parseCookingCategory(req.getCkgCategory()));
    }
    if (req.getCkgKnd() != null) {
      p.setCkgKnd(parseCookingKind(req.getCkgKnd()));
    }

    if (req.getCkgMtrlCn() != null) {
      p.setCkgMtrlCn(req.getCkgMtrlCn());
    }
    if (req.getCkgInbun() != null) {
      p.setCkgInbun(req.getCkgInbun());
    }
    if (req.getCkgLevel() != null) {
      p.setCkgLevel(req.getCkgLevel());
    }
    if (req.getCkgTime() != null) {
      p.setCkgTime(req.getCkgTime());
    }

    if (req.getRcpImgUrl() != null) {
      p.setRcpImgUrl(req.getRcpImgUrl());
    }
    if (req.getRcpSteps() != null) {
      p.setRcpSteps(req.getRcpSteps());
    }
    if (req.getRcpStepsImg() != null) {
      p.setRcpStepsImg(req.getRcpStepsImg());
    }

    p.setUpdatedAt(LocalDateTime.now()); // @UpdateTimestamp 있으면 생략 가능

    return toDto(p);
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

    postRepo.delete(p);
  }
}
