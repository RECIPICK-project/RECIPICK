package SITE.RECIPICK.RECIPICK_PROJECT.service;

import static SITE.RECIPICK.RECIPICK_PROJECT.util.PostMapper.toDto;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDTO;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostUpdateRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.Post;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.PostRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 내 레시피(임시글) 수정/삭제 커맨드 서비스 - 읽기 전용이 아닌 "상태 변경"을 수행하므로 @Transactional 필수 - 보안/권한 체크(소유자), 비즈니스
 * 제약(임시글만) 먼저 통과시킨 뒤 변경
 */
@Service
@RequiredArgsConstructor
public class MyPostCommandService {

  private final PostRepository postRepo;

  /**
   * 내 임시 레시피 부분 수정
   * <p>
   * 1) postId로 게시글 로드 (없으면 404에 매핑될 예외 던짐) 2) 작성자(나) 소유인지 확인 (아니면 403) 3) 임시글인지 확인 (정식이면 409) 4) 요청
   * 바디에 들어온 "널이 아닌 필드"만 부분 업데이트 5) updatedAt 갱신 → 트랜잭션 종료 시 JPA 더티체킹으로 UPDATE 발생
   *
   * @param me     현재 로그인 사용자 ID
   * @param postId 수정 대상 게시글 ID
   * @param req    수정할 필드들(널 허용 → 부분수정)
   * @return 수정 후 DTO (클라이언트 응답용)
   * <p>
   * 예외(메시지 → 전역 예외처리기에서 상태코드 매핑 권장): - "POST_NOT_FOUND"      → 404 Not Found - "FORBIDDEN" → 403
   * Forbidden (타인이 소유) - "ONLY_TEMP_EDITABLE"  → 409 Conflict (정식글은 수정 불가)
   */
  @Transactional
  public PostDTO updateMyTempPost(Integer me, Long postId, PostUpdateRequest req) {
    // 1) 대상 로드 (없으면 실패)
    Post p = postRepo.findById(postId)
        .orElseThrow(() -> new IllegalArgumentException("POST_NOT_FOUND"));

    // 2) 소유자 체크 (나의 글이 아니면 금지)
    if (!p.getUser().getId().equals(me)) {
      throw new IllegalStateException("FORBIDDEN");
    }

    // 3) 임시글만 수정 가능 (정식이면 거부)
    if (p.isRcpIsOfficial()) {
      throw new IllegalStateException("ONLY_TEMP_EDITABLE");
    }

    // 4) 부분수정: 요청에 들어온 값만 반영 (널은 무시)
    if (req.getTitle() != null) {
      p.setTitle(req.getTitle());
    }
    if (req.getFoodName() != null) {
      p.setFoodName(req.getFoodName());
    }
    if (req.getCkgMth() != null) {
      p.setCkgMth(req.getCkgMth());
    }
    if (req.getCkgCategory() != null) {
      p.setCkgCategory(req.getCkgCategory());
    }
    if (req.getCkgKnd() != null) {
      p.setCkgKnd(req.getCkgKnd());
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

    // 5) 타임스탬프 갱신 (감지된 변경과 함께 UPDATE)
    p.setUpdatedAt(LocalDateTime.now());

    // 영속 엔티티이므로 save() 불필요
    return toDto(p);
  }

  /**
   * 내 임시 레시피 삭제
   * <p>
   * 절차: 1) postId로 게시글 로드 (없으면 404) 2) 작성자(나) 소유인지 확인 (아니면 403) 3) 임시글인지 확인 (정식이면 409) 4) 삭제 수행
   * <p>
   * 예외: - "POST_NOT_FOUND"      → 404 Not Found - "FORBIDDEN"           → 403 Forbidden -
   * "ONLY_TEMP_DELETABLE" → 409 Conflict
   */
  @Transactional
  public void deleteMyTempPost(Integer me, Long postId) {
    Post p = postRepo.findById(postId)
        .orElseThrow(() -> new IllegalArgumentException("POST_NOT_FOUND"));

    if (!p.getUser().getId().equals(me)) {
      throw new IllegalStateException("FORBIDDEN");
    }
    if (p.isRcpIsOfficial()) {
      throw new IllegalStateException("ONLY_TEMP_DELETABLE");
    }

    postRepo.delete(p);
  }
}
