package SITE.RECIPICK.RECIPICK_PROJECT.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * PostDTO
 * <p>
 * ✅ 역할 - POST 엔티티의 데이터를 API 응답/요청으로 전달하기 위한 DTO (Data Transfer Object) - 엔티티를 직접 노출하지 않고, 필요한 필드만
 * 골라서 안전하게 반환
 * <p>
 * ✅ 특징 - @Getter : 모든 필드에 대한 getter 자동 생성 (JSON 직렬화 시 사용됨) - @Builder : 빌더 패턴 제공
 * (PostDTO.builder()...) - @AllArgsConstructor : 모든 필드를 받는 생성자 자동 생성 - @NoArgsConstructor  : 파라미터
 * 없는 기본 생성자 자동 생성
 * <p>
 * ⚠️ 주의 - 엔티티와 1:1로 매핑돼 있지만, 나중에 필요 없는 필드는 빼거나 추가 가공 필드도 넣을 수 있음 (예: 작성자 닉네임, 댓글 개수 등)
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostDTO {

  private Long postId;              // 게시글 ID (PK)
  private String title;             // 제목
  private String foodName;          // 음식명
  private int viewCount;            // 조회수
  private int likeCount;            // 좋아요 수
  private String ckgMth;            // 조리 방법
  private String ckgCategory;       // 조리 카테고리
  private String ckgKnd;            // 조리 종류
  private String ckgMtrlCn;         // 재료 내용 (텍스트)
  private int ckgInbun;             // 인분 수
  private int ckgLevel;             // 난이도
  private int ckgTime;              // 조리 시간(분 등)
  private String rcpImgUrl;         // 대표 이미지 URL
  private String rcpSteps;          // 조리 단계 (텍스트)
  private String rcpStepsImg;       // 단계별 이미지 URL (텍스트)
  private boolean rcpIsOfficial;    // 정식 레시피 여부 (false=임시, true=정식)
  private LocalDateTime createdAt;  // 작성 시각
  private LocalDateTime updatedAt;  // 수정 시각
  private int reportCount;          // 신고 횟수
}
