package SITE.RECIPICK.RECIPICK_PROJECT.service;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.PostRepository;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 레시피 게시글 관련 비즈니스 로직을 처리하는 서비스 클래스
 * <p>
 * 주요 기능: - 레시피 저장, 조회, 수정, 삭제 - 레시피 검색 및 필터링 - 좋아요, 조회수 관리
 */
@Slf4j  // Lombok의 로깅 기능 활성화 - log.debug(), log.info() 등 사용 가능
@Service  // Spring의 서비스 계층 컴포넌트로 등록 - 비즈니스 로직 처리 담당
@RequiredArgsConstructor  // final 필드에 대한 생성자를 자동 생성 - 의존성 주입을 위해 사용
public class PostService {

  /**
   * 데이터베이스 접근을 위한 Repository 의존성 주입 final로 선언하여 불변성 보장 및 RequiredArgsConstructor를 통한 생성자 주입
   */
  private final PostRepository postRepository;

  /**
   * 새로운 레시피를 데이터베이스에 저장하는 메서드
   *
   * @param postDto 클라이언트로부터 받은 레시피 정보 (DTO)
   * @return PostDto 저장된 레시피 정보 (자동 생성된 ID, 타임스탬프 포함)
   * @throws RuntimeException 데이터베이스 저장 실패 시
   */
  @Transactional  // 데이터베이스 트랜잭션 관리 - 메서드 실행 중 오류 발생 시 자동 롤백
  public PostDto saveRecipe(PostDto postDto) {
    // DEBUG 레벨 로그 - 개발/테스트 환경에서 상세 추적용
    log.debug("레시피 저장 시작 - 제목: {}", postDto.getTitle());

    // 단계별 설명 (List<String> → "1. 내용 | 2. 내용 | ..." 변환)
    String formattedSteps = IntStream.range(0, postDto.getRcpSteps().size())
        .mapToObj(i -> (i + 1) + ". " + postDto.getRcpSteps().get(i))
        .collect(Collectors.joining(" | "));

// 단계별 이미지도 동일하게 처리 가능
    String formattedStepImgs = String.join(" | ", postDto.getRcpStepsImg());

    // ===== 1단계: DTO → Entity 변환 =====
    // 클라이언트에서 받은 DTO를 데이터베이스 저장용 Entity로 변환
    // Builder 패턴 사용으로 가독성과 유지보수성 향상
    PostEntity postEntity = PostEntity.builder()
        // === 기본 정보 ===
        .userId(1)                             // (임시 고정값)
        .title(postDto.getTitle())            // 레시피 제목 (필수)
        .foodName(postDto.getFoodName())      // 음식 이름 (필수)

        // === 분류 정보 (Enum 타입) ===
        .ckgMth(postDto.getCkgMth())          // 조리방법 (끓이기, 굽기, 튀기기 등)
        .ckgCategory(postDto.getCkgCategory()) // 요리 카테고리 (한식, 중식, 일식 등)
        .ckgKnd(postDto.getCkgKnd())          // 요리 종류 (밥류, 면류, 국/탕 등)

        // === 레시피 상세 정보 ===
        .ckgMtrlCn(postDto.getCkgMtrlCn())    // 재료 내용 ("|" 구분자로 저장)
        .ckgInbun(postDto.getCkgInbun())      // 몇 인분 (숫자)
        .ckgLevel(postDto.getCkgLevel())      // 조리 난이도 (1~5)
        .ckgTime(postDto.getCkgTime())        // 조리 시간 (분 단위)

        // === 이미지 및 설명 ===
        .rcpImgUrl(postDto.getRcpImgUrl())    // 썸네일 이미지 URL
        .rcpSteps(formattedSteps)         // ✅ 수정된 부분
        .rcpStepsImg(formattedStepImgs)   // ✅ 수정된 부분

        // === 상태 정보 ===
//        .rcpIsOfficial(postDto.getRcpIsOfficial()) // 정식 레시피 여부 (0: 임시, 1: 정식)

        // 주의: postId, viewCount, likeCount, reportCount, createdAt, updatedAt은
        // Entity에서 자동 생성되므로 여기서 설정하지 않음
        .build();

    // ===== 2단계: 데이터베이스 저장 =====
    // JPA Repository의 save() 메서드 호출
    // - 새 레코드인 경우: INSERT 쿼리 실행
    // - 기존 레코드 수정인 경우: UPDATE 쿼리 실행 (현재는 항상 새 레코드)
    // - @CreationTimestamp, @UpdateTimestamp가 자동으로 현재 시간 설정
    // - @GeneratedValue에 의해 postId가 자동 생성되어 반환
    PostEntity savedEntity = postRepository.save(postEntity);

    // INFO 레벨 로그 - 운영 환경에서도 중요한 작업 완료 기록
    log.info("레시피 저장 완료 - ID: {}, 제목: {}", savedEntity.getPostId(), savedEntity.getTitle());

    // ===== 3단계: Entity → DTO 변환 후 반환 =====
    // 데이터베이스에서 저장된 Entity를 클라이언트 응답용 DTO로 변환
    // 자동 생성된 정보들(ID, 타임스탬프, 기본값)을 포함하여 반환
    return PostDto.builder()

        // === 클라이언트에서 전달받은 정보 (그대로 반환) ===
        .title(savedEntity.getTitle())
        .foodName(savedEntity.getFoodName())
        .ckgMth(savedEntity.getCkgMth())
        .ckgCategory(savedEntity.getCkgCategory())
        .ckgKnd(savedEntity.getCkgKnd())
        .ckgMtrlCn(savedEntity.getCkgMtrlCn())
        .ckgInbun(savedEntity.getCkgInbun())
        .ckgLevel(savedEntity.getCkgLevel())
        .ckgTime(savedEntity.getCkgTime())
        .rcpImgUrl(savedEntity.getRcpImgUrl())
        .rcpSteps(savedEntity.getRcpSteps())
        .rcpStepsImg(savedEntity.getRcpStepsImg())
//        .rcpIsOfficial(savedEntity.getRcpIsOfficial())
        .build();

    // 반환된 PostDto에는:
    // 1. 클라이언트가 보낸 모든 정보
    // 2. 데이터베이스에서 자동 생성된 ID
    // 3. 자동 설정된 타임스탬프
    // 4. 기본값들 (조회수, 좋아요, 신고횟수 = 0)
    // 이 모든 정보가 포함되어 Controller로 전달됨
  }
}
