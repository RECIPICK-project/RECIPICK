package SITE.RECIPICK.RECIPICK_PROJECT.service;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDTO;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.PostRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.util.PostMapper;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 내가 "좋아요한 레시피(Post)" 관련 조회 기능을 담당하는 서비스 계층
 * <p>
 * 컨트롤러(MyLikeController)에서 호출되며, Repository → Entity(Post) → DTO(PostDTO) 변환 과정을 담당한다.
 */
@Service
@RequiredArgsConstructor // final 필드를 생성자 주입으로 자동 처리
public class MyLikeService {

  // ===== 의존성 주입 =====
  private final PostRepository postRepo; // Post 데이터를 가져오는 Repository

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
  public List<PostDTO> getMyLikedPosts(Integer me, int offset, int limit) {
    // 1. offset/limit 기반 페이지네이션 객체 생성
    var pageable = PageRequest.of(offset / Math.max(1, limit), Math.max(1, limit));

    // 2. Repository에서 "내가 좋아요한 Post" 조회
    var posts = postRepo.findLikedPosts(me, pageable);

    // 3. 엔티티(Post) → DTO(PostDTO) 변환 후 리스트로 반환
    return posts.stream()
        .map(PostMapper::toDto)
        .collect(Collectors.toList());
  }
}
