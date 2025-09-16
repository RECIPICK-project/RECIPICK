package SITE.RECIPICK.RECIPICK_PROJECT.service;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.Ingredient;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.RecipeIngredient;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.UserEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.IngredientRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.PostRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.RecipeIngredientRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.UserRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.util.CurrentUser;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
  private final IngredientRepository ingredientRepository;
  private final RecipeIngredientRepository recipeIngredientRepository;
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

    // 재료 및 RecipeIngredient 저장 (분리된 데이터로 저장)
    saveIngredients(savedEntity.getPostId(), postDto.getIngredientNames(),
                   postDto.getIngredientQuantities(), postDto.getIngredientUnits());

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


  // 재료 및 RecipeIngredient 저장 메서드 (분리된 데이터 사용)
  private void saveIngredients(Integer postId, List<String> ingredientNames,
                               List<String> quantities, List<String> units) {
    if (ingredientNames == null || ingredientNames.isEmpty()) {
      log.warn("재료 목록이 비어있습니다 - postId: {}", postId);
      return;
    }

    log.debug("재료 저장 시작 - postId: {}, 재료 수: {}", postId, ingredientNames.size());

    // 목록 크기 검증
    int ingredientCount = ingredientNames.size();
    if (quantities != null && quantities.size() != ingredientCount) {
      log.warn("재료와 수량 목록 크기가 다름 - 재료: {}, 수량: {}", ingredientCount, quantities.size());
    }
    if (units != null && units.size() != ingredientCount) {
      log.warn("재료와 단위 목록 크기가 다름 - 재료: {}, 단위: {}", ingredientCount, units.size());
    }

    for (int i = 0; i < ingredientCount; i++) {
      String ingredientName = ingredientNames.get(i);
      if (ingredientName == null || ingredientName.trim().isEmpty()) {
        continue;
      }

      ingredientName = ingredientName.trim();

      // 수량과 단위 조합하여 amount 생성
      String quantity = (quantities != null && i < quantities.size()) ?
          quantities.get(i).trim() : "";
      String unit = (units != null && i < units.size()) ?
          units.get(i).trim() : "";

      // amount = quantity + unit (예: "2" + "개" = "2개", "300" + "g" = "300g")
      String amount = "";
      if (!quantity.isEmpty() && !unit.isEmpty()) {
        amount = quantity + unit;
      } else if (!quantity.isEmpty()) {
        amount = quantity;
      } else if (!unit.isEmpty()) {
        amount = unit;
      }

      // 재료명에 따라 자동으로 카테고리 결정
      String ingredientSort = getIngredientCategoryByName(ingredientName);

      // Ingredient 찾기 또는 생성 (재료명만 저장)
      Ingredient ingredient = findOrCreateIngredient(ingredientName, ingredientSort);

      // RecipeIngredient 생성 (수량 정보 저장)
      RecipeIngredient recipeIngredient = new RecipeIngredient();
      recipeIngredient.setPostId(postId);
      recipeIngredient.setIngId(ingredient.getIngId());
      recipeIngredient.setAmount(amount);

      recipeIngredientRepository.save(recipeIngredient);

      log.debug("RecipeIngredient 저장 완료 - postId: {}, ingId: {}, 재료명: {}, 수량: {}, 단위: {}, amount: {}, 자동분류: {}",
          postId, ingredient.getIngId(), ingredientName, quantity, unit, amount, ingredientSort);
    }

    log.info("모든 재료 저장 완료 - postId: {}, 저장된 재료 수: {}", postId, ingredientCount);
  }

  // 재료명에 따라 자동으로 카테고리 분류하는 메서드 (PostEntity.CookingCategory 기준)
  private String getIngredientCategoryByName(String ingredientName) {
    String name = ingredientName.toLowerCase().trim();

    // 소고기 (BEEF) - 가장 구체적인 것부터 먼저 체크
    if (name.contains("소고기") || name.contains("한우") || name.contains("갈비") ||
        name.contains("등심") || name.contains("안심") || name.contains("불고기") ||
        name.contains("소갈비") || name.contains("차돌박이") || name.contains("양지") ||
        name.contains("사태") || name.contains("우둔") || name.contains("설도")) {
      return "소고기";
    }

    // 돼지고기 (PORK)
    if (name.contains("돼지고기") || name.contains("삼겹살") || name.contains("목살") ||
        name.contains("앞다리") || name.contains("뒷다리") || name.contains("등갈비") ||
        name.contains("돼지갈비") || name.contains("베이컨") || name.contains("햄") ||
        name.contains("소시지") || name.contains("족발") || name.contains("순대") ||
        name.contains("항정살") || name.contains("가브리살")) {
      return "돼지고기";
    }

    // 닭고기 (CHICKEN)
    if (name.contains("닭고기") || name.contains("닭") || name.contains("닭다리") ||
        name.contains("닭가슴살") || name.contains("닭날개") || name.contains("닭봉") ||
        name.contains("치킨") || name.contains("닭안심") || name.contains("닭껍질") ||
        name.contains("닭발") || name.contains("닭목") || name.contains("영계")) {
      return "닭고기";
    }

    // 기타 육류 (MEAT) - 위에서 분류되지 않은 육류들
    if (name.contains("양고기") || name.contains("오리고기") || name.contains("거위고기") ||
        name.contains("칠면조") || name.contains("토끼고기") || name.contains("사슴고기") ||
        name.contains("말고기") || name.contains("염소고기") || name.contains("고기") ||
        name.contains("육회") || name.contains("간") || name.contains("내장")) {
      return "육류";
    }

    // 쌀 (RICE) - 밀가루보다 먼저 체크
    if (name.contains("쌀") || name.contains("백미") || name.contains("현미") ||
        name.contains("찹쌀") || name.contains("멥쌀") || name.contains("흑미") ||
        name.contains("적미") || name.contains("밥") || name.contains("누룽지")) {
      return "쌀";
    }

    // 밀가루 (FLOUR)
    if (name.contains("밀가루") || name.contains("강력분") || name.contains("중력분") ||
        name.contains("박력분") || name.contains("통밀가루") || name.contains("호밀가루") ||
        name.contains("글루텐") || name.contains("전분") || name.contains("옥수수전분") ||
        name.contains("감자전분") || name.contains("타피오카")) {
      return "밀가루";
    }

    // 버섯류 (MUSHROOM)
    if (name.contains("버섯") || name.contains("표고버섯") || name.contains("느타리버섯") ||
        name.contains("팽이버섯") || name.contains("새송이버섯") || name.contains("양송이버섯") ||
        name.contains("목이버섯") || name.contains("송이버섯") || name.contains("석이버섯") ||
        name.contains("만가닥버섯") || name.contains("마른버섯")) {
      return "버섯류";
    }

    // 해물류 (SEAFOOD)
    if (name.contains("생선") || name.contains("물고기") || name.contains("새우") ||
        name.contains("오징어") || name.contains("문어") || name.contains("조개") ||
        name.contains("전복") || name.contains("굴") || name.contains("게") ||
        name.contains("연어") || name.contains("참치") || name.contains("고등어") ||
        name.contains("명태") || name.contains("갈치") || name.contains("꽁치") ||
        name.contains("조기") || name.contains("광어") || name.contains("농어") ||
        name.contains("도미") || name.contains("삼치") || name.contains("방어")) {
      return "해물류";
    }

    // 채소류 (VEGETABLES) - 버섯은 이미 분류했으므로 제외
    if (name.contains("양파") || name.contains("마늘") || name.contains("생강") ||
        name.contains("당근") || name.contains("감자") || name.contains("고구마") ||
        name.contains("무") || name.contains("배추") || name.contains("상추") ||
        name.contains("시금치") || name.contains("브로콜리") || name.contains("오이") ||
        name.contains("토마토") || name.contains("파프리카") || name.contains("고추") ||
        name.contains("대파") || name.contains("쪽파") || name.contains("파") ||
        name.contains("애호박") || name.contains("호박") || name.contains("가지") ||
        name.contains("콩나물") || name.contains("숙주") || name.contains("미나리") ||
        name.contains("깻잎") || name.contains("부추") || name.contains("셀러리") ||
        name.contains("양배추") || name.contains("케일") || name.contains("피망")) {
      return "채소류";
    }

    // 곡류 (GRAINS) - 쌀과 밀가루는 이미 분류했으므로 제외
    if (name.contains("보리") || name.contains("귀리") || name.contains("옥수수") ||
        name.contains("수수") || name.contains("메밀") || name.contains("퀴노아") ||
        name.contains("통밀") || name.contains("율무") || name.contains("조") ||
        name.contains("기장") || name.contains("쌀알") || name.contains("곡물")) {
      return "곡류";
    }

    // 달걀/유제품 (EGG_DAIRY)
    if (name.contains("계란") || name.contains("달걀") || name.contains("우유") ||
        name.contains("치즈") || name.contains("버터") || name.contains("요구르트") ||
        name.contains("요거트") || name.contains("생크림") || name.contains("크림") ||
        name.contains("모차렐라") || name.contains("파마산") || name.contains("체다") ||
        name.contains("까망베르") || name.contains("마스카포네") || name.contains("리코타")) {
      return "달걀/유제품";
    }

    // 콩/견과류 (BEANS_NUTS)
    if (name.contains("콩") || name.contains("두부") || name.contains("된장") ||
        name.contains("간장") || name.contains("고추장") || name.contains("땅콩") ||
        name.contains("호두") || name.contains("아몬드") || name.contains("잣") ||
        name.contains("깨") || name.contains("참깨") || name.contains("들깨") ||
        name.contains("검은깨") || name.contains("피스타치오") || name.contains("캐슈넛") ||
        name.contains("피칸") || name.contains("밤") || name.contains("은행")) {
      return "콩/견과류";
    }

    // 과일류 (FRUITS)
    if (name.contains("사과") || name.contains("배") || name.contains("바나나") ||
        name.contains("오렌지") || name.contains("귤") || name.contains("레몬") ||
        name.contains("라임") || name.contains("포도") || name.contains("딸기") ||
        name.contains("키위") || name.contains("망고") || name.contains("파인애플") ||
        name.contains("복숭아") || name.contains("자두") || name.contains("살구") ||
        name.contains("체리") || name.contains("블루베리") || name.contains("수박") ||
        name.contains("참외") || name.contains("멜론")) {
      return "과일류";
    }

    // 건어물류 (DRIED_SEAFOOD)
    if (name.contains("멸치") || name.contains("다시마") || name.contains("미역") ||
        name.contains("김") || name.contains("마른") || name.contains("건") ||
        name.contains("북어") || name.contains("오징어채") || name.contains("새우젓") ||
        name.contains("젓갈") || name.contains("액젓") || name.contains("마른오징어") ||
        name.contains("건새우") || name.contains("마른명태") || name.contains("황태")) {
      return "건어물류";
    }

    // 가공식품류 (PROCESSED_FOOD)
    if (name.contains("라면") || name.contains("국수") || name.contains("파스타") ||
        name.contains("스파게티") || name.contains("우동") || name.contains("냉면") ||
        name.contains("소면") || name.contains("당면") || name.contains("떡") ||
        name.contains("만두") || name.contains("빵") || name.contains("케이크") ||
        name.contains("쿠키") || name.contains("과자") || name.contains("캔") ||
        name.contains("통조림") || name.contains("소스") || name.contains("케첩") ||
        name.contains("마요네즈") || name.contains("드레싱") || name.contains("인스턴트") ||
        name.contains("냉동") || name.contains("레토르트")) {
      return "가공식품류";
    }

    // 기본값 (OTHER)
    return "기타";
  }

  // 재료 찾기 또는 생성 메서드
  private Ingredient findOrCreateIngredient(String name, String sort) {
    // 이름으로 기존 재료 검색
    List<Ingredient> existingIngredients = ingredientRepository.findByName(name);

    if (!existingIngredients.isEmpty()) {
      Ingredient existing = existingIngredients.get(0);

      // 기존 재료의 카테고리가 없거나 다른 경우 업데이트
      if (existing.getSort() == null || existing.getSort().trim().isEmpty() || !existing.getSort().equals(sort)) {
        existing.setSort(sort);
        existing = ingredientRepository.save(existing);
        log.debug("기존 재료 카테고리 업데이트 - 이름: {}, 카테고리: {}", name, sort);
      }

      return existing;
    }

    // 새 재료 생성
    Ingredient newIngredient = new Ingredient();
    newIngredient.setName(name);
    newIngredient.setSort(sort);

    Ingredient savedIngredient = ingredientRepository.save(newIngredient);
    log.debug("새 재료 생성 - ID: {}, 이름: {}, 카테고리: {}",
        savedIngredient.getIngId(), name, sort);

    return savedIngredient;
  }

  /**
   * 전체 레시피 조회 (페이징 지원)
   *
   * @param page 페이지 번호 (0부터 시작)
   * @param size 페이지 크기
   * @param sortBy 정렬 기준 (createdAt, likeCount, viewCount 등)
   * @param sortDirection 정렬 방향 (ASC, DESC)
   * @param official 정식 레시피 여부 (1: 정식, 0: 임시, null: 전체)
   * @param category 카테고리 필터 (선택적)
   * @param method 조리방법 필터 (선택적)
   * @return 페이징된 레시피 목록과 메타데이터
   */
  public Map<String, Object> getAllRecipes(
      int page,
      int size,
      String sortBy,
      String sortDirection,
      Integer official,
      String category,
      String method) {

    log.debug("전체 레시피 조회 - page: {}, size: {}, sortBy: {}, direction: {}, official: {}, category: {}, method: {}",
        page, size, sortBy, sortDirection, official, category, method);

    // 정렬 설정
    Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
    Sort sort = Sort.by(direction, sortBy);
    Pageable pageable = PageRequest.of(page, size, sort);

    // 전체 레시피 조회 (필터링 적용)
    Page<PostEntity> postPage = getFilteredPosts(pageable, official, category, method);

    // Entity -> DTO 변환
    List<PostDto> recipeDtos = postPage.getContent().stream()
        .map(this::convertToDto)
        .collect(Collectors.toList());

    // 응답 데이터 구성
    Map<String, Object> response = new HashMap<>();
    response.put("recipes", recipeDtos);
    response.put("totalElements", postPage.getTotalElements());
    response.put("totalPages", postPage.getTotalPages());
    response.put("currentPage", postPage.getNumber());
    response.put("pageSize", postPage.getSize());
    response.put("hasNext", postPage.hasNext());
    response.put("hasPrevious", postPage.hasPrevious());
    response.put("isFirst", postPage.isFirst());
    response.put("isLast", postPage.isLast());

    log.info("전체 레시피 조회 완료 - 총 {}개 중 {}페이지 ({}-{}) 반환",
        postPage.getTotalElements(), page + 1,
        page * size + 1, Math.min((page + 1) * size, (int) postPage.getTotalElements()));

    return response;
  }

  /**
   * 필터링된 레시피 조회 (내부 메서드)
   */
  private Page<PostEntity> getFilteredPosts(Pageable pageable, Integer official, String category, String method) {
    // 기본적으로 모든 레시피 조회
    if (official == null && category == null && method == null) {
      return postRepository.findAll(pageable);
    }

    // 정식 레시피만 조회하는 경우가 가장 일반적
    if (official != null && category == null && method == null) {
      return postRepository.findByRcpIsOfficial(official, pageable);
    }

    // TODO: 복잡한 필터링의 경우 Repository에 커스텀 쿼리 메서드 추가 필요
    // 현재는 간단한 필터링만 지원
    log.warn("복잡한 필터링은 아직 지원하지 않습니다. 기본 조회를 수행합니다.");
    return postRepository.findAll(pageable);
  }

  /**
   * 인기 레시피 조회 (좋아요 순)
   */
  public List<PostDto> getPopularRecipes(int limit) {
    log.debug("인기 레시피 조회 - limit: {}", limit);

    Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "likeCount"));
    List<PostEntity> popularPosts = postRepository.findByRcpIsOfficial(1, pageable).getContent();

    List<PostDto> result = popularPosts.stream()
        .map(this::convertToDto)
        .collect(Collectors.toList());

    log.info("인기 레시피 {}개 조회 완료", result.size());
    return result;
  }

  /**
   * 최신 레시피 조회
   */
  public List<PostDto> getLatestRecipes(int limit) {
    log.debug("최신 레시피 조회 - limit: {}", limit);

    Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
    List<PostEntity> latestPosts = postRepository.findByRcpIsOfficial(1, pageable).getContent();

    List<PostDto> result = latestPosts.stream()
        .map(this::convertToDto)
        .collect(Collectors.toList());

    log.info("최신 레시피 {}개 조회 완료", result.size());
    return result;
  }

  /**
   * 개별 레시피 상세 조회 (postId로 조회)
   */
  public PostDto getRecipeById(Integer postId) {
    log.debug("개별 레시피 조회 - postId: {}", postId);

    PostEntity postEntity = postRepository.findByPostId(postId)
        .orElseThrow(() -> new IllegalArgumentException("해당 레시피를 찾을 수 없습니다. ID: " + postId));

    // 조회수 증가
    postEntity.setViewCount(postEntity.getViewCount() + 1);
    postRepository.save(postEntity);

    PostDto result = convertToDtoForDetail(postEntity);
    log.info("레시피 상세 조회 완료 - ID: {}, 제목: {}, 조회수: {}",
        postId, result.getTitle(), postEntity.getViewCount());

    return result;
  }

  /**
   * 상세 페이지용 DTO 변환 (프론트엔드 호환성을 위해 추가 정보 포함)
   */
  private PostDto convertToDtoForDetail(PostEntity entity) {
    // 기본 변환
    PostDto dto = convertToDto(entity);

    // 프론트엔드 호환성을 위한 추가 설정
    dto.setPostId(entity.getPostId());
    dto.setAuthor(entity.getUserNickname()); // author 필드 추가
    dto.setUserId(entity.getUserId());
    dto.setUserEmail(entity.getUserEmail());
    dto.setCreatedAt(entity.getCreatedAt());
    dto.setViewCount(entity.getViewCount());
    dto.setLikeCount(entity.getLikeCount());

    // 재료를 파이프 구분자 형태로 변환 (프론트엔드 호환성)
    if (dto.getCkgMtrlCn() != null && !dto.getCkgMtrlCn().isEmpty()) {
      String ingredientsForFrontend = String.join("|", dto.getCkgMtrlCn());
      dto.setIngredientsString(ingredientsForFrontend);
    }

    // 조리시간을 "XX min" 형태로 변환
    if (dto.getCkgTime() != null) {
      dto.setCookingTimeString(dto.getCkgTime() + " min");
    }

    log.debug("상세 페이지용 DTO 변환 완료 - postId: {}", entity.getPostId());
    return dto;
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