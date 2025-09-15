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

    // ✅ 모든 필수 필드 유효성 검사
    // 1. 문자열 필드 - null, 빈 문자열, 공백만 있는 경우 체크
    if (postDto.getTitle() == null || postDto.getTitle().trim().isEmpty()) {
      throw new IllegalArgumentException("레시피 제목은 필수입니다");
    }
    if (postDto.getFoodName() == null || postDto.getFoodName().trim().isEmpty()) {
      throw new IllegalArgumentException("음식 이름은 필수입니다");
    }

    // 2. Enum 필드 - null 체크
    if (postDto.getCkgMth() == null) {
      throw new IllegalArgumentException("조리방법을 선택해주세요");
    }
    if (postDto.getCkgCategory() == null) {
      throw new IllegalArgumentException("요리 카테고리를 선택해주세요");
    }
    if (postDto.getCkgKnd() == null) {
      throw new IllegalArgumentException("요리 종류를 선택해주세요");
    }
    if (postDto.getCkgInbun() == null) {
      throw new IllegalArgumentException("몇 인분인지 선택해주세요");
    }
    if (postDto.getCkgLevel() == null) {
      throw new IllegalArgumentException("조리 난이도를 선택해주세요");
    }
    if (postDto.getCkgTime() == null) {
      throw new IllegalArgumentException("조리 시간을 선택해주세요");
    }

    // 3. List 필드 - null이거나 빈 배열인 경우 체크
    if (postDto.getCkgMtrlCn() == null || postDto.getCkgMtrlCn().isEmpty()) {
      throw new IllegalArgumentException("재료를 최소 1개 이상 입력해주세요");
    }
    if (postDto.getRcpSteps() == null || postDto.getRcpSteps().isEmpty()) {
      throw new IllegalArgumentException("조리 과정을 최소 1단계 이상 입력해주세요");
    }

    // 4. List 내부 빈 값 체크
    boolean hasEmptyIngredient =
        postDto.getCkgMtrlCn().stream()
            .anyMatch(ingredient -> ingredient == null || ingredient.trim().isEmpty());
    if (hasEmptyIngredient) {
      throw new IllegalArgumentException("빈 재료가 포함되어 있습니다. 모든 재료를 입력해주세요");
    }

    boolean hasEmptyStep =
        postDto.getRcpSteps().stream()
            .anyMatch(step -> step == null || step.trim().isEmpty());
    if (hasEmptyStep) {
      throw new IllegalArgumentException("빈 조리 과정이 포함되어 있습니다. 모든 단계를 입력해주세요");
    }

    // 단계별 설명 (List<String> → "1. 내용 | 2. 내용 | ..." 변환)
    String formattedSteps =
        IntStream.range(0, postDto.getRcpSteps().size())
            .mapToObj(i -> (i + 1) + ". " + postDto.getRcpSteps().get(i))
            .collect(Collectors.joining(" | "));

    // 단계별 이미지도 동일하게 처리 (null 체크 추가)
    String formattedStepImgs =
        (postDto.getRcpStepsImg() != null && !postDto.getRcpStepsImg().isEmpty())
            ? String.join(" | ", postDto.getRcpStepsImg())
            : "";

    // 재료 리스트를 문자열로 변환 (앞에 [재료] 추가)
    String formattedIngredients = "[재료] " + String.join(" | ", postDto.getCkgMtrlCn());

    // 썸네일 이미지 필수 검증
    if (postDto.getRcpImgUrl() == null || postDto.getRcpImgUrl().trim().isEmpty()) {
      throw new IllegalArgumentException("썸네일 이미지는 필수입니다");
    }
    String imageUrl = postDto.getRcpImgUrl();

    // 현재 로그인한 사용자 정보 가져오기 (완전 수정 버전)
    Integer userId = null;
    String userEmail = null;
    UserEntity userEntity = null;

    try {
      log.debug("=== 사용자 정보 조회 시작 ===");

      // 방법 1: CurrentUser 유틸리티 클래스 사용
      userEmail = currentUser.email();
      userId = currentUser.userId();

      log.debug("CurrentUser에서 조회 - userId: {}, userEmail: {}", userId, userEmail);

      // 사용자 존재 여부 확인 및 닉네임 조회
      if (userId != null) {
        userEntity = userRepository.findById(userId)
            .orElse(null);
      }

      // CurrentUser로 조회 실패하거나 사용자가 없는 경우 SecurityContext 사용
      if (userEntity == null) {
        log.warn("CurrentUser로 사용자 조회 실패, SecurityContext 사용");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
          final String emailFromAuth = authentication.getName(); // final 변수로 생성
          userEmail = emailFromAuth; // 값 할당
          log.debug("SecurityContext에서 email 조회: {}", userEmail);

          // 이메일로 사용자 조회 (final 변수 사용)
          userEntity = userRepository.findByEmail(emailFromAuth)
              .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + emailFromAuth));
          userId = userEntity.getUserId();

          log.debug("SecurityContext를 통한 사용자 조회 성공 - userId: {}, userEmail: {}", userId, userEmail);
        } else {
          throw new IllegalStateException("인증되지 않은 사용자입니다");
        }
      }

      log.debug("최종 사용자 정보 - ID: {}, Email: {}, Nickname: {}",
          userId, userEmail, userEntity.getNickname());

    } catch (Exception e) {
      log.error("사용자 정보 조회 완전 실패: {}", e.getMessage(), e);
      throw new IllegalStateException("사용자 정보를 조회할 수 없습니다", e);
    }

    // 사용자 정보 최종 검증
    if (userId == null || userEmail == null || userEntity == null) {
      throw new IllegalStateException("사용자 정보가 불완전합니다");
    }

    // String → Enum 변환
    PostEntity.CookingMethod cookingMethod = convertToCookingMethod(postDto.getCkgMth());
    PostEntity.CookingCategory cookingCategory =
        convertToCookingCategory(postDto.getCkgCategory());
    PostEntity.CookingKind cookingKind = convertToCookingKind(postDto.getCkgKnd());

    // DTO → Entity 변환 (변환된 enum 사용)
    log.debug("Entity 생성 전 값 확인 - userId: {}, userNickname: {}, userEmail: {}",
        userId, userEntity.getNickname(), userEmail);

    PostEntity postEntity =
        PostEntity.builder()
            .userId(userId)
            .userNickname(userEntity.getNickname())
            .userEmail(userEmail)
            .title(postDto.getTitle())
            .foodName(postDto.getFoodName())
            .ckgMth(cookingMethod) // String → enum 변환
            .ckgCategory(cookingCategory) // String → enum 변환
            .ckgKnd(cookingKind) // String → enum 변환
            .ckgMtrlCn(formattedIngredients)
            .ckgInbun(postDto.getCkgInbun()) // enum → enum (그대로)
            .ckgLevel(postDto.getCkgLevel()) // enum → enum (그대로)
            .ckgTime(postDto.getCkgTime()) // enum → enum (그대로)
            .rcpImgUrl(imageUrl)
            .rcpSteps(formattedSteps)
            .rcpStepsImg(formattedStepImgs)
            .build();

    // 최종 저장 전 로그 - getter로 다시 확인
    log.debug("PostEntity 생성 완료 - userId: {}, userNickname: {}, userEmail: {}",
        postEntity.getUserId(), postEntity.getUserNickname(), postEntity.getUserEmail());

    // 빌더가 제대로 동작했는지 필드별 확인
    log.debug("PostEntity 필드 검증:");
    log.debug("- getUserId(): {}", postEntity.getUserId());
    log.debug("- getUserNickname(): {}", postEntity.getUserNickname());
    log.debug("- getUserEmail(): {}", postEntity.getUserEmail());
    log.debug("- getTitle(): {}", postEntity.getTitle());

    // 데이터베이스 저장 전 최종 확인
    log.debug("=== 데이터베이스 저장 시작 ===");

    // 데이터베이스 저장
    PostEntity savedEntity = postRepository.save(postEntity);

    // 저장 후 DB에서 조회된 실제 값 확인
    log.debug("=== 데이터베이스 저장 완료 후 실제 저장된 값 확인 ===");
    log.debug("- savedEntity.getPostId(): {}", savedEntity.getPostId());
    log.debug("- savedEntity.getUserId(): {}", savedEntity.getUserId());
    log.debug("- savedEntity.getUserNickname(): '{}'", savedEntity.getUserNickname());
    log.debug("- savedEntity.getUserEmail(): '{}'", savedEntity.getUserEmail());
    log.debug("- savedEntity.getTitle(): '{}'", savedEntity.getTitle());
    log.debug("- savedEntity.getFoodName(): '{}'", savedEntity.getFoodName());
    log.debug("- savedEntity.getCkgInbun(): {}", savedEntity.getCkgInbun());
    log.debug("- savedEntity.getCkgLevel(): {}", savedEntity.getCkgLevel());
    log.debug("- savedEntity.getCkgTime(): {}", savedEntity.getCkgTime());
    log.debug("- savedEntity.getCreatedAt(): {}", savedEntity.getCreatedAt());
    log.debug("- savedEntity.getUpdatedAt(): {}", savedEntity.getUpdatedAt());

    // DB에 저장된 후 null 값 특별 확인
    if (savedEntity.getUserNickname() == null) {
      log.error("❌ DB 저장 후에도 userNickname이 null입니다!");
    } else {
      log.info("✅ userNickname 정상 저장: '{}'", savedEntity.getUserNickname());
    }

    if (savedEntity.getUserEmail() == null) {
      log.error("❌ DB 저장 후에도 userEmail이 null입니다!");
    } else {
      log.info("✅ userEmail 정상 저장: '{}'", savedEntity.getUserEmail());
    }

    log.info("=== 레시피 저장 완료 ===");
    log.info("레시피 ID: {}, 제목: '{}', 작성자: '{}' ({})",
        savedEntity.getPostId(),
        savedEntity.getTitle(),
        savedEntity.getUserNickname(),
        savedEntity.getUserEmail());

    // Entity → DTO 변환 후 반환
    // ✅ 재료 파싱 수정: "[재료] " 접두사 제거 후 분리
    String ingredientsText = savedEntity.getCkgMtrlCn();
    List<String> ingredientsList;
    if (ingredientsText.startsWith("[재료] ")) {
      ingredientsList = Arrays.asList(ingredientsText.substring(4).split(" \\| "));
    } else {
      ingredientsList = Arrays.asList(ingredientsText.split(" \\| "));
    }

    // ✅ 조리 단계 파싱 수정: "1. ", "2. " 등 번호 제거
    List<String> stepsList =
        Arrays.stream(savedEntity.getRcpSteps().split(" \\| "))
            .map(step -> step.replaceFirst("^\\d+\\.\\s*", "")) // 앞의 "1. " 제거
            .collect(Collectors.toList());

    return PostDto.builder()
        .title(savedEntity.getTitle())
        .foodName(savedEntity.getFoodName())
        .ckgMth(savedEntity.getCkgMth().getDescription()) // enum → String 변환
        .ckgCategory(savedEntity.getCkgCategory().getDescription()) // enum → String 변환
        .ckgKnd(savedEntity.getCkgKnd().getDescription()) // enum → String 변환
        .ckgMtrlCn(ingredientsList) // 재료 파싱 수정
        .ckgInbun(savedEntity.getCkgInbun()) // enum → enum (그대로)
        .ckgLevel(savedEntity.getCkgLevel()) // enum → enum (그대로)
        .ckgTime(savedEntity.getCkgTime()) // enum → enum (그대로)
        .rcpImgUrl(savedEntity.getRcpImgUrl())
        .rcpSteps(stepsList) // 단계 파싱 수정
        .rcpStepsImg(
            savedEntity.getRcpStepsImg().isEmpty()
                ? List.of()
                : Arrays.asList(savedEntity.getRcpStepsImg().split(" \\| ")))
        .build();
  }

  // String → CookingMethod Enum 변환
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

  // String → CookingCategory Enum 변환
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

  // String → CookingKind Enum 변환
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

  // Integer → CookingInbun Enum 변환 (DB에서 조회할 때 사용)
  private PostEntity.CookingInbun convertIntegerToInbunEnum(Integer value) {
      if (value == null) {
          return PostEntity.CookingInbun.ONE;
      }

    for (PostEntity.CookingInbun inbun : PostEntity.CookingInbun.values()) {
      if (inbun.getValue().equals(value)) {
        return inbun;
      }
    }
    return PostEntity.CookingInbun.ONE;
  }

  // Integer → CookingLevel Enum 변환 (DB에서 조회할 때 사용)
  private PostEntity.CookingLevel convertIntegerToLevelEnum(Integer value) {
      if (value == null) {
          return PostEntity.CookingLevel.LEVEL_1;
      }

    for (PostEntity.CookingLevel level : PostEntity.CookingLevel.values()) {
      if (level.getValue().equals(value)) {
        return level;
      }
    }
    return PostEntity.CookingLevel.LEVEL_1;
  }

  // Integer → CookingTime Enum 변환 (DB에서 조회할 때 사용)
  private PostEntity.CookingTime convertIntegerToTimeEnum(Integer minutes) {
      if (minutes == null) {
          return PostEntity.CookingTime.TIME_5;
      }

    for (PostEntity.CookingTime time : PostEntity.CookingTime.values()) {
      if (time.getMinutes().equals(minutes)) {
        return time;
      }
    }
    return PostEntity.CookingTime.TIME_5;
  }
}