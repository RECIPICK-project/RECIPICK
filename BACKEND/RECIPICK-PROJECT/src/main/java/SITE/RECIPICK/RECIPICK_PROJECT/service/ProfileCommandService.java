package SITE.RECIPICK.RECIPICK_PROJECT.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.ProfileImageUpdateRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;

/**
 * 프로필 수정 관련 서비스 계층
 *
 * <p>컨트롤러(ProfileCommandController)에서 호출되며 - 프로필 이미지 변경 을 담당한다.
 */
@Service
@RequiredArgsConstructor // final 필드 생성자 자동 주입
public class ProfileCommandService {

    private final ProfileRepository profileRepo; // PROFILE 테이블 접근용 Repository

    /**
     * 프로필 이미지 변경
     *
     * @param me 현재 로그인한 사용자 ID
     * @param req 클라이언트 요청 DTO (profileImg)
     *     <p>1) 프로필 조회 - profileRepo.findById(me) → 없으면 PROFILE_NOT_FOUND
     *     <p>2) 입력값 검증 - req.getProfileImg() == null 또는 공백 → PROFILE_IMG_REQUIRED
     *     <p>3) 값 반영 - 문자열 trim() 후 저장 - updatedAt = 현재 시각
     *     <p>4) 저장 - @Transactional 덕분에 엔티티 변경감지가 일어나 UPDATE 쿼리 자동 실행
     *     <p>발생 가능한 예외: - IllegalArgumentException("PROFILE_NOT_FOUND") → 사용자 프로필 없음 -
     *     IllegalArgumentException("PROFILE_IMG_REQUIRED") → 이미지 값 누락
     */
    @Transactional
    public void changeProfileImage(Integer me, ProfileImageUpdateRequest req) {
        // 1. 프로필 로드 (없으면 예외)
        var pr =
                profileRepo
                        .findById(me)
                        .orElseThrow(() -> new IllegalArgumentException("PROFILE_NOT_FOUND"));

        // 2. 요청 값 검증
        if (req.getProfileImg() == null || req.getProfileImg().isBlank()) {
            throw new IllegalArgumentException("PROFILE_IMG_REQUIRED");
        }

        // 3. 프로필 이미지 변경 + 수정일 갱신
        pr.setProfileImg(req.getProfileImg().trim());
        pr.setUpdatedAt(LocalDateTime.now());
    }
}
