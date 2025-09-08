package SITE.RECIPICK.RECIPICK_PROJECT.service;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.PostRepository;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

  private final PostRepository postRepository;

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

    // 단계별 설명 (List<String> → "1. 내용 | 2. 내용 | ..." 변환)
    String formattedSteps = IntStream.range(0, postDto.getRcpSteps().size())
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

    // String → Enum 변환
    PostEntity.CookingMethod cookingMethod = convertToCookingMethod(postDto.getCkgMth());
    PostEntity.CookingCategory cookingCategory = convertToCookingCategory(postDto.getCkgCategory());  
    PostEntity.CookingKind cookingKind = convertToCookingKind(postDto.getCkgKnd());

    // DTO → Entity 변환 (변환된 enum 사용)
    PostEntity postEntity = PostEntity.builder()
        .userId(1)
        .title(postDto.getTitle())
        .foodName(postDto.getFoodName())
        .ckgMth(cookingMethod)           // String → enum 변환
        .ckgCategory(cookingCategory)    // String → enum 변환
        .ckgKnd(cookingKind)            // String → enum 변환
        .ckgMtrlCn(formattedIngredients)
        .ckgInbun(postDto.getCkgInbun())       // enum → enum (그대로)
        .ckgLevel(postDto.getCkgLevel())       // enum → enum (그대로)
        .ckgTime(postDto.getCkgTime())         // enum → enum (그대로)
        .rcpImgUrl(imageUrl)
        .rcpSteps(formattedSteps)
        .rcpStepsImg(formattedStepImgs)
        .build();

    // 데이터베이스 저장
    PostEntity savedEntity = postRepository.save(postEntity);

    log.info("레시피 저장 완료 - ID: {}, 제목: {}", savedEntity.getPostId(), savedEntity.getTitle());

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
    List<String> stepsList = Arrays.stream(savedEntity.getRcpSteps().split(" \\| "))
        .map(step -> step.replaceFirst("^\\d+\\.\\s*", "")) // 앞의 "1. " 제거
        .collect(Collectors.toList());

    return PostDto.builder()
        .title(savedEntity.getTitle())
        .foodName(savedEntity.getFoodName())
        .ckgMth(savedEntity.getCkgMth().getDescription())           // enum → String 변환
        .ckgCategory(savedEntity.getCkgCategory().getDescription()) // enum → String 변환
        .ckgKnd(savedEntity.getCkgKnd().getDescription())           // enum → String 변환
        .ckgMtrlCn(ingredientsList)               // 재료 파싱 수정
        .ckgInbun(savedEntity.getCkgInbun())       // enum → enum (그대로)
        .ckgLevel(savedEntity.getCkgLevel())       // enum → enum (그대로)
        .ckgTime(savedEntity.getCkgTime())         // enum → enum (그대로)
        .rcpImgUrl(savedEntity.getRcpImgUrl())
        .rcpSteps(stepsList)                       // 단계 파싱 수정
        .rcpStepsImg(savedEntity.getRcpStepsImg().isEmpty()
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
}