package SITE.RECIPICK.RECIPICK_PROJECT.service;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.ProfileEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.UserEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.ProfileRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileInitService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    // ✅ 이미 확인한 userId를 메모리에 캐싱 (서버 재시작 시 초기화)
    private final Set<Integer> checkedUserIds = ConcurrentHashMap.newKeySet();

    @Transactional
    public void ensureProfileExists(Integer userId) {
        // ✅ 이미 확인한 사용자면 바로 리턴 (DB 조회 없음)
        if (checkedUserIds.contains(userId)) {
            return;
        }

        // 이미 존재하면 아무것도 하지 않음
        if (profileRepository.existsById(userId)) {
            checkedUserIds.add(userId);  // ✅ 캐시에 추가
            return;
        }

        try {
            // 관리되는 User 엔티티 가져오기
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            // 새 Profile 생성
            ProfileEntity profile = new ProfileEntity();
            profile.setUserEntity(user);  // @MapsId가 자동으로 userId 설정
            profile.setNickname(user.getNickname());
            profile.setLatestAt(LocalDateTime.now());
            profile.setUpdatedAt(LocalDateTime.now());

            profileRepository.save(profile);
            checkedUserIds.add(userId);  // ✅ 캐시에 추가
            log.info("ProfileEntity 생성 완료 - userId={}, nickname={}", userId, user.getNickname());

        } catch (Exception e) {
            // 동시성 문제로 이미 생성된 경우
            if (profileRepository.existsById(userId)) {
                checkedUserIds.add(userId);  // ✅ 캐시에 추가
                log.warn("ProfileEntity 생성 시도 중 예외 발생했으나 이미 존재함 - userId={}", userId);
                return;
            }
            // 다른 문제라면 로그만 남기고 무시
            log.error("ProfileEntity 생성 실패 - userId={}, error={}", userId, e.getMessage());
        }
    }
}