package SITE.RECIPICK.RECIPICK_PROJECT.util;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDTO;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity;

/**
 * PostMapper
 * <p>
 * ✅ 역할 - 엔티티(Post) ↔ DTO(PostDTO) 변환 담당 (컨트롤러/서비스의 중복 코드 제거)
 * <p>
 * ✅ 원칙 - toDto: 엔티티를 API 응답용 DTO로 풀매핑 - toEntity: "등록/수정 요청"에 해당하는 입력 필드만 선택 매핑 (집계/상태/시간 필드는 여기서
 * 세팅하지 않고, 서비스 계층/엔티티 라이프사이클에서 관리)
 * <p>
 * ⚠️ 주의 - NPE 방어: 인자 null 시 null 반환 - createdAt/updatedAt은 @PrePersist/@PreUpdate로 관리하는 걸 기본으로, 정말
 * 필요할 때만 수동 세팅
 */
public final class PostMapper {

  // 유틸 클래스: 생성자 막기
  private PostMapper() {
  }

  /**
   * 엔티티(Post) → DTO(PostDTO) - 조회 응답에 필요한 대부분의 필드를 매핑
   */
  public static PostDTO toDto(PostEntity p) {
    if (p == null) {
      return null;
    }

    return PostDTO.builder()
        .postId(p.getPostId())
        .title(p.getTitle())
        .foodName(p.getFoodName())
        .viewCount(p.getViewCount())
        .likeCount(p.getLikeCount())
        .ckgMth(p.getCkgMth())
        .ckgCategory(p.getCkgCategory())
        .ckgKnd(p.getCkgKnd())
        .ckgMtrlCn(p.getCkgMtrlCn())
        .ckgInbun(p.getCkgInbun() == null ? 0 : p.getCkgInbun())
        .ckgLevel(p.getCkgLevel() == null ? 0 : p.getCkgLevel())
        .ckgTime(p.getCkgTime() == null ? 0 : p.getCkgTime())
        .rcpImgUrl(p.getRcpImgUrl())
        .rcpSteps(p.getRcpSteps())
        .rcpStepsImg(p.getRcpStepsImg())
        .rcpIsOfficial(p.isRcpIsOfficial())
        .createdAt(p.getCreatedAt())
        .updatedAt(p.getUpdatedAt())
        .reportCount(p.getReportCount())
        .build();
  }

  /**
   * DTO(PostDTO) → 엔티티(Post)
   * <p>
   * 💡 용도 - "등록/수정" 입력 DTO를 엔티티로 옮길 때 사용. - 카운트/신고/정식여부/시간 같은 **서버 관리 필드**는 여기서 건드리지 않음.
   * <p>
   * ⚠️ 주의 - PK(postId)는 DB가 생성하므로 세팅하지 않음. - createdAt/updatedAt은 보통 엔티티 훅(@PrePersist/@PreUpdate)로
   * 관리. - 필요 시, 서비스 계층에서만 엄격히 통제하여 세팅하세요.
   */
  public static PostEntity toEntity(PostDTO dto) {
    if (dto == null) {
      return null;
    }

    PostEntity p = new PostEntity();

    // === 입력용(클라이언트가 바꿀 수 있는 내용)만 매핑 ===
    p.setTitle(dto.getTitle());
    p.setFoodName(dto.getFoodName());
    p.setCkgMth(dto.getCkgMth());
    p.setCkgCategory(dto.getCkgCategory());
    p.setCkgKnd(dto.getCkgKnd());
    p.setCkgMtrlCn(dto.getCkgMtrlCn());
    p.setCkgInbun(dto.getCkgInbun());
    p.setCkgLevel(dto.getCkgLevel());
    p.setCkgTime(dto.getCkgTime());
    p.setRcpImgUrl(dto.getRcpImgUrl());
    p.setRcpSteps(dto.getRcpSteps());
    p.setRcpStepsImg(dto.getRcpStepsImg());

    // === 서버 관리 필드: 여기서는 세팅하지 않음 ===
    // - viewCount / likeCount / reportCount: 집계 로직으로만 변경
    // - rcpIsOfficial: 서비스에서 publish() 같은 메서드로 상태 변경
    // - createdAt / updatedAt: 엔티티 라이프사이클로 관리 (@PrePersist/@PreUpdate)

    return p;
  }
}
