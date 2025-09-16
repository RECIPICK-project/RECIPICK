package SITE.RECIPICK.RECIPICK_PROJECT.service;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.MyProfileResponse;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.NicknameUpdateRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.PostRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.ProfileRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.ReviewRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.UserRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.util.PostMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 📌 마이페이지 관련 서비스
 * <p>
 * 컨트롤러(MyPageController)에서 호출되며, - 프로필 조회 - 닉네임 변경 기능을 제공한다.
 * <p>
 * 조회 시에는 게시글/리뷰/댓글 집계까지 포함된 DTO(MyProfileResponse)를 반환한다.
 */
@Service                   // 스프링 컴포넌트 스캔 시 Bean 등록
@RequiredArgsConstructor   // final 필드 생성자 자동 생성(생성자 주입)
public class MyPageService {

  // ===== 의존 Repository =====
  private final ProfileRepository profileRepo;   // 프로필 정보 조회/수정
  private final PostRepository postRepo;         // 내가 올린 정식 레시피/좋아요 집계
  private final ReviewRepository reviewRepo;     // 내가 쓴 리뷰 집계
  private final UserRepository userRepo;

  /**
   * ✅ [GET /me/profile]
   * <p>
   * 특정 사용자(me)의 마이페이지 프로필 데이터를 조회한다.
   *
   * @param me 사용자 ID (현재는 임시로 Integer, 로그인 붙이면 Security 컨텍스트에서 가져옴)
   * @return MyProfileResponse DTO (JSON 직렬화되어 클라이언트 응답)
   * <p>
   * 1. PROFILE 테이블에서 닉네임/등급/이미지 조회 (없으면 예외) 2. POST 테이블에서 내가 올린 정식 레시피 개수 + 좋아요 총합 집계 3. REVIEW /
   * COMMENT 테이블에서 내가 작성한 개수 집계 4. MyProfileResponse DTO로 묶어 반환
   */
  @Transactional(readOnly = true)
  public MyProfileResponse getMyProfile(Integer me) {
    // 1) 프로필 조회 (없으면 404 대신 IllegalArgumentException 발생)
    var pr = profileRepo.findById(me)
        .orElseThrow(() -> new IllegalArgumentException("PROFILE_NOT_FOUND"));

    // 2) 게시글 집계
    long myRecipeCount = postRepo.countPublishedByAuthor(me); // 정식 레시피 개수
    long totalLikesOnMyPosts = postRepo.sumLikesOnUsersPublished(me); // 좋아요 합계

    // 3) 리뷰 + 댓글 집계
    long reviewCount = reviewRepo.countByUserUserId(me);
    long commentCount;
    try {
      commentCount = reviewRepo.countByUserUserId(me);
    } catch (Exception e) {
      commentCount = 0; // COMMENT 테이블이 없거나 초기화 전이면 안전하게 0 처리
    }

    // 4) DTO로 반환
    return new MyProfileResponse(
        pr.getNickname(),          // 닉네임
        pr.getGrade().name(),      // 등급 (enum → 문자열)
        pr.getProfileImg(),        // 프로필 이미지
        myRecipeCount,             // 내가 올린 정식 레시피 개수
        totalLikesOnMyPosts,       // 좋아요 총합
        reviewCount + commentCount // 활동 수
    );
  }

  /**
   * ✅ [PATCH /me/profile/nickname]
   * <p>
   * 닉네임 변경 기능 (7일 쿨다운 적용)
   * <p>
   * 규칙: - 닉네임은 공백 불가 / 50자 이내 - 기존 닉네임과 동일하면 변경 없음 - updated_at 기준 7일 이내 변경 시도 →
   * 거부(NICKNAME_CHANGE_COOLDOWN) - 이미 존재하는 닉네임이면 거부(NICKNAME_DUPLICATED) - 위 조건 통과 시 닉네임 변경 +
   * updated_at 갱신
   */
  @Transactional
  public void changeNickname(Integer me, NicknameUpdateRequest req) {
    // 0) 입력 검증
    String raw = req.getNewNickname();
    if (raw == null || raw.trim().isEmpty()) {
      throw new IllegalArgumentException("NICKNAME_REQUIRED");
    }
    String newNickname = raw.trim();
    if (newNickname.length() > 50) {
      throw new IllegalArgumentException("NICKNAME_TOO_LONG");
    }

    // 1) 프로필 로딩
    var pr = profileRepo.findById(me)
        .orElseThrow(() -> new IllegalArgumentException("PROFILE_NOT_FOUND"));

    // 2) 동일 닉이면 조용히 종료(변경 없음)
    if (newNickname.equals(pr.getNickname())) {
      return;
    }

    // 3) 7일 쿨다운 (updated_at 기준)
    var lastUpdated = pr.getUpdatedAt();
    if (lastUpdated != null && lastUpdated.isAfter(java.time.LocalDateTime.now().minusDays(7))) {
      throw new IllegalStateException("NICKNAME_CHANGE_COOLDOWN");
    }

    // 4) 중복 검사 (profile 기준)
    if (profileRepo.existsByNickname(newNickname)) {
      throw new IllegalStateException("NICKNAME_DUPLICATED");
    }

    // 5) 변경 및 저장 (profile)
    pr.setNickname(newNickname);
    pr.setUpdatedAt(java.time.LocalDateTime.now());
    profileRepo.save(pr);

    // 6) 동기화 (users)
    var user = userRepo.findById(me)
        .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
    user.setNickname(newNickname);
    userRepo.save(user);
  }


  /**
   * 내가 좋아요한 레시피 목록을 조회한다.
   *
   * @param me     현재 로그인한 사용자 ID
   * @param offset 페이지네이션 시작 지점 (몇 번째 데이터부터 가져올지)
   * @param limit  한 페이지에 가져올 데이터 개수
   * @return 사용자가 좋아요한 레시피(PostDTO)의 리스트
   * <p>
   * 1) offset과 limit을 기반으로 PageRequest 생성 - PageRequest.of(page, size) 구조인데, page = offset / limit
   * 으로 환산 - Math.max(1, limit) → limit이 0 들어와도 최소 1개는 조회하도록 방어
   * <p>
   * 2) postRepo.findLikedPosts(me, pageable) 호출 - 내부적으로 JOIN(LikeTable + Post) 쿼리 실행 - 내가 좋아요한 Post
   * 엔티티 목록 반환
   * <p>
   * 3) Entity(Post) → DTO(PostDTO) 변환 - PostMapper::toDto 메서드로 매핑 -
   * stream().map(...).collect(toList()) 형태
   * <p>
   * 4) 최종적으로 JSON 응답에 쓰일 DTO 리스트 리턴
   * <p>
   * 트랜잭션 속성:
   * @Transactional(readOnly = true) → SELECT 전용, 성능 최적화
   */
  @Transactional(readOnly = true)
  public List<PostDto> getMyLikedPosts(Integer me, int offset, int limit) {
    // 1. offset/limit 기반 페이지네이션 객체 생성
    var pageable = PageRequest.of(offset / Math.max(1, limit), Math.max(1, limit));

    // 3. 엔티티(Post) → DTO(PostDTO) 변환 후 리스트로 반환
    return postRepo.findLikedPosts(me, pageable).stream()
        .map(PostMapper::toDto)
        .toList();
  }

  @RestControllerAdvice  // 전역 예외 처리기
  public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
      if ("PROFILE_NOT_FOUND".equals(e.getMessage())) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
      }
      return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }
  }
}
