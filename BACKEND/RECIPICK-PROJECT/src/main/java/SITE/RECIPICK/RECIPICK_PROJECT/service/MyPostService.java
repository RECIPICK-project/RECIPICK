package SITE.RECIPICK.RECIPICK_PROJECT.service;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.PostRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.util.PostMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 내 게시글(정식/임시) 목록 조회 서비스
 * <p>
 * 컨트롤러(MyPostController)에서 호출되며, - offset/limit → PageRequest 변환 - Repository에서 조건(정식/임시, 최신순)으로 조회
 * - Post 엔티티 → PostDTO 매핑 의 과정을 수행한다.
 */
@Service
@RequiredArgsConstructor
public class MyPostService {

  private final PostRepository postRepo; // Post 읽기 전용 접근

  /**
   * 내가 작성한 "정식 레시피" 목록을 최신순으로 조회
   *
   * @param me     로그인 유저 ID
   * @param offset 몇 번째부터 가져올지 (커서 느낌)
   * @param limit  한 번에 몇 개 가져올지
   * @return PostDTO 리스트(정식 레시피, createdAt DESC)
   * <p>
   * 1) PageRequest.of(page, size) 생성 - page = offset / max(1, limit) - size = max(1, limit)  (0
   * 들어와도 방어) 2) postRepo.findByUserIdAndRcpIsOfficialTrueOrderByCreatedAtDesc(...) - 내 글 중
   * rcp_is_official = true 인 것만 - created_at 내림차순 3) 엔티티 → DTO 매핑 후 반환
   */
  @Transactional(readOnly = true)
  public List<PostDto> getMyOfficialPosts(Integer me, int offset, int limit) {
    var page = PageRequest.of(offset / Math.max(limit, 1), Math.max(limit, 1));
    return postRepo.findByUserIdAndRcpIsOfficialOrderByCreatedAtDesc(me, 1, page)
        .stream().map(PostMapper::toDto).toList();
  }

  /**
   * 내가 작성한 "임시 레시피" 목록을 최신순으로 조회
   *
   * @param me     로그인 유저 ID
   * @param offset 몇 번째부터 가져올지
   * @param limit  한 번에 몇 개
   * @return PostDTO 리스트(임시 레시피, createdAt DESC)
   * <p>
   * 구현 포인트는 getMyOfficialPosts와 동일하되, rcp_is_official = false 조건을 사용한다.
   */
  @Transactional(readOnly = true)
  public List<PostDto> getMyTempPosts(Integer me, int offset, int limit) {
    var page = PageRequest.of(offset / Math.max(limit, 1), Math.max(limit, 1));
    return postRepo.findByUserIdAndRcpIsOfficialOrderByCreatedAtDesc(me, 0, page)
        .stream().map(PostMapper::toDto).toList();
  }
}
