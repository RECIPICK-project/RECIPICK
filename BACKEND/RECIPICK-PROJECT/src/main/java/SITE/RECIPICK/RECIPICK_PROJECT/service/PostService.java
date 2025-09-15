package SITE.RECIPICK.RECIPICK_PROJECT.service;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.UserEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.PostRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.UserRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.util.CurrentUser;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final CurrentUser currentUser;

  @Transactional
  public PostDto saveRecipe(PostDto postDto) {
    log.debug("레시피 저장 시작 - 제목: {}", postDto.getTitle());

    // 모든 필수 필드 유효성 검사
    validatePostDto(postDto);

    // 단계별 설명 변환
    String formattedSteps =
        IntStream.range(0, postDto.getRcpSteps().size())
            .mapToObj(i -> (i + 1) + ". " + postDto.getRcpSteps().get(i))
            .collect(Collectors.joining(" | "));

    // 단계별 이미지 처리
    String formattedStepImgs =
        (postDto.getRcpStepsImg() != null && !postDto.getRcpStepsImg().isEmpty())
            ? String.join(" | ", postDto.getRcpStepsImg())
            : "";

    // 재료 리스트 변환
    String formattedIngredients = "[재료] " + String.join(" | ", postDto.getCkgMtrlCn());

    // 썸네일 이미지 검증
    if (postDto.getRcpImgUrl() == null || postDto.getRcpImgUrl().trim().isEmpty()) {
      throw new IllegalArgumentException("썸네일 이미지는 필수입니다");
    }
    String imageUrl = postDto.getRcpImgUrl();

    // 현재 로그인한 사용자 정보 가져오기
    UserInfo userInfo = getCurrentUserInfo();

    // String → Enum 변환
    PostEntity.CookingMethod cookingMethod = convertToCookingMethod(postDto.getCkgMth());
    PostEntity.CookingCategory cookingCategory = convertToCookingCategory(postDto.getCkgCategory());
    PostEntity.CookingKind cookingKind = convertToCookingKind(postDto.getCkgKnd());

    // DTO → Entity 변환
    PostEntity postEntity =
        PostEntity.builder()
            .userId(userInfo.userId)
            .userNickname(userInfo.nickname)
            .userEmail(userInfo.email)
            .title(postDto.getTitle())
            .foodName(postDto.getFoodName())
            .ckgMth(cookingMethod)
            .ckgCategory(cookingCategory)
            .ckgKnd(cookingKind)
            .ckgMtrlCn(formattedIngredients)
            .ckgInbun(postDto.getCkgInbun())
            .ckgLevel(postDto.getCkgLevel())
            .ckgTime(postDto.getCkgTime())
            .rcpImgUrl(imageUrl)
            .rcpSteps(formattedSteps)
            .rcpStepsImg(formattedStepImgs)
            .build();

    // 데이터베이스 저장
    PostEntity savedEntity = postRepository.save(postEntity);

    log.info("레시피 저장 완료 - ID: {}, 제목: {}, 작성자: {}",
        savedEntity.getPostId(), savedEntity.getTitle(), savedEntity.getUserNickname());

    // Entity → DTO 변환 후 반환
    return convertToDto(savedEntity);
  }

  // 유효성 검사 메서드
  private void validatePostDto(PostDto postDto) {
    // 문자열 필드 검사
    if (postDto.getTitle() == null || postDto.getTitle().trim().isEmpty()) {
      throw new IllegalArgumentException("레시피 제목은 필수입니다");
    }
    if (postDto.getFoodName() == null || postDto.getFoodName().trim().isEmpty()) {
      throw new IllegalArgumentException("음식 이름은 필수입니다");
    }

    // String 필드 검사
    if (postDto.getCkgMth() == null) {
      throw new IllegalArgumentException("조리방법을 선택해주세요");
    }
    if (postDto.getCkgCategory() == null) {
      throw new IllegalArgumentException("요리 카테고리를 선택해주세요");
    }
    if (postDto.getCkgKnd() == null) {
      throw new IllegalArgumentException("요리 종류를 선택해주세요");
    }

    // Integer 필드 검사
    if (postDto.getCkgInbun() == null) {
      throw new IllegalArgumentException("몇 인분인지 선택해주세요");
    }
    if (postDto.getCkgLevel() == null) {
      throw new IllegalArgumentException("조리 난이도를 선택해주세요");
    }
    if (postDto.getCkgTime() == null) {
      throw new IllegalArgumentException("조리 시간을 선택해주세요");
    }

    // List 필드 검사
    if (postDto.getCkgMtrlCn() == null || postDto.getCkgMtrlCn().isEmpty()) {
      throw new IllegalArgumentException("재료를 최소 1개 이상 입력해주세요");
    }
    if (postDto.getRcpSteps() == null || postDto.getRcpSteps().isEmpty()) {
      throw new IllegalArgumentException("조리 과정을 최소 1단계 이상 입력해주세요");
    }

    // List 내부 빈 값 검사
    boolean hasEmptyIngredient = postDto.getCkgMtrlCn().stream()
        .anyMatch(ingredient -> ingredient == null || ingredient.trim().isEmpty());
    if (hasEmptyIngredient) {
      throw new IllegalArgumentException("빈 재료가 포함되어 있습니다. 모든 재료를 입력해주세요");
    }

    boolean hasEmptyStep = postDto.getRcpSteps().stream()
        .anyMatch(step -> step == null || step.trim().isEmpty());
    if (hasEmptyStep) {
      throw new IllegalArgumentException("빈 조리 과정이 포함되어 있습니다. 모든 단계를 입력해주세요");
    }
  }

  // 사용자 정보 조회 메서드
  private UserInfo getCurrentUserInfo() {
    try {
      // CurrentUser 유틸리티 클래스 사용
      String userEmail = currentUser.email();
      Integer userId = currentUser.userId();

      UserEntity userEntity = null;
      if (userId != null) {
        userEntity = userRepository.findById(userId).orElse(null);
      }

      // CurrentUser로 조회 실패 시 SecurityContext 사용
      if (userEntity == null) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
          final String emailFromAuth = authentication.getName();
          userEmail = emailFromAuth;

          userEntity = userRepository.findByEmail(emailFromAuth)
              .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + emailFromAuth));
          userId = userEntity.getUserId();
        } else {
          throw new IllegalStateException("인증되지 않은 사용자입니다");
        }
      }

      // 사용자 정보 최종 검증
      if (userId == null || userEmail == null || userEntity == null) {
        throw new IllegalStateException("사용자 정보가 불완전합니다");
      }

      return new UserInfo(userId, userEmail, userEntity.getNickname());

    } catch (Exception e) {
      log.error("사용자 정보 조회 실패: {}", e.getMessage(), e);
      throw new IllegalStateException("사용자 정보를 조회할 수 없습니다", e);
    }
  }

  // Entity → DTO 변환 메서드
  private PostDto convertToDto(PostEntity savedEntity) {
    // 재료 파싱
    String ingredientsText = savedEntity.getCkgMtrlCn();
    List<String> ingredientsList;
    if (ingredientsText.startsWith("[재료] ")) {
      ingredientsList = Arrays.asList(ingredientsText.substring(4).split(" \\| "));
    } else {
      ingredientsList = Arrays.asList(ingredientsText.split(" \\| "));
    }

    // 조리 단계 파싱
    List<String> stepsList =
        Arrays.stream(savedEntity.getRcpSteps().split(" \\| "))
            .map(step -> step.replaceFirst("^\\d+\\.\\s*", ""))
            .collect(Collectors.toList());

    return PostDto.builder()
        .title(savedEntity.getTitle())
        .foodName(savedEntity.getFoodName())
        .ckgMth(savedEntity.getCkgMth().getDescription())
        .ckgCategory(savedEntity.getCkgCategory().getDescription())
        .ckgKnd(savedEntity.getCkgKnd().getDescription())
        .ckgMtrlCn(ingredientsList)
        .ckgInbun(savedEntity.getCkgInbun())
        .ckgLevel(savedEntity.getCkgLevel())
        .ckgTime(savedEntity.getCkgTime())
        .rcpImgUrl(savedEntity.getRcpImgUrl())
        .rcpSteps(stepsList)
        .rcpStepsImg(
            savedEntity.getRcpStepsImg().isEmpty()
                ? List.of()
                : Arrays.asList(savedEntity.getRcpStepsImg().split(" \\| ")))
        .build();
  }

  // String → Enum 변환 메서드들
  private PostEntity.CookingMethod convertToCookingMethod(String methodStr) {
    if (methodStr == null || methodStr.trim().isEmpty()) {
      return PostEntity.CookingMethod.OTHER;
    }

    for (PostEntity.CookingMethod method : PostEntity.CookingMethod.values()) {
      if (method.getDescription().equals(methodStr.trim())) {
        return method;
      }
    }

    log.warn("알 수 없는 조리방법: {}, OTHER로 설정", methodStr);
    return PostEntity.CookingMethod.OTHER;
  }

  private PostEntity.CookingCategory convertToCookingCategory(String categoryStr) {
    if (categoryStr == null || categoryStr.trim().isEmpty()) {
      return PostEntity.CookingCategory.OTHER;
    }

    for (PostEntity.CookingCategory category : PostEntity.CookingCategory.values()) {
      if (category.getDescription().equals(categoryStr.trim())) {
        return category;
      }
    }

    log.warn("알 수 없는 카테고리: {}, OTHER로 설정", categoryStr);
    return PostEntity.CookingCategory.OTHER;
  }

  private PostEntity.CookingKind convertToCookingKind(String kindStr) {
    if (kindStr == null || kindStr.trim().isEmpty()) {
      return PostEntity.CookingKind.OTHER;
    }

    for (PostEntity.CookingKind kind : PostEntity.CookingKind.values()) {
      if (kind.getDescription().equals(kindStr.trim())) {
        return kind;
      }
    }

    log.warn("알 수 없는 요리 종류: {}, OTHER로 설정", kindStr);
    return PostEntity.CookingKind.OTHER;
  }

  // 사용자 정보를 담는 내부 클래스
  private static class UserInfo {

    final Integer userId;
    final String email;
    final String nickname;

    UserInfo(Integer userId, String email, String nickname) {
      this.userId = userId;
      this.email = email;
      this.nickname = nickname;
    }
  }
}