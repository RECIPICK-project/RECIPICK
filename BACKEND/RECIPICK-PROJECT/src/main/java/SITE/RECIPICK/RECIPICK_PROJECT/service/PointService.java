package SITE.RECIPICK.RECIPICK_PROJECT.service;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.ProfileEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.UserEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.UserGrade;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.ProfileRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PointService {

    // 등급별 필요 포인트
    private static final int BRONZE_THRESHOLD = 0;
    private static final int SILVER_THRESHOLD = 100;
    private static final int GOLD_THRESHOLD = 300;
    private static final int PLATINUM_THRESHOLD = 800;
    private static final int DIAMOND_THRESHOLD = 2000;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    /**
     * 포인트 적립 및 등급 업데이트
     */
    public void addPoints(Integer userId, PointAction action) {
        try {
            log.info("포인트 적립 시작 - userId: {}, action: {}, points: {}",
                    userId, action.name(), action.getPoints());

            ProfileEntity profile = getOrCreateProfile(userId);

            // 포인트 적립
            int currentPoints = profile.getPoint() != null ? profile.getPoint() : 0;
            int newPoints = currentPoints + action.getPoints();
            profile.setPoint(newPoints);

            // 등급 재평가
            UserGrade newGrade = calculateGrade(newPoints);
            UserGrade currentGrade = profile.getGrade();

            if (currentGrade != newGrade) {
                profile.setGrade(newGrade);
                log.info("등급 승급! - userId: {}, {} -> {}, 포인트: {}",
                        userId, currentGrade, newGrade, newPoints);
            }

            profileRepository.save(profile);

            log.info("포인트 적립 완료 - userId: {}, 총 포인트: {}, 등급: {}",
                    userId, newPoints, profile.getGrade());

        } catch (Exception e) {
            log.error("포인트 적립 실패 - userId: {}, action: {}", userId, action, e);
            // 포인트 적립 실패해도 메인 기능에 영향주지 않도록 예외를 던지지 않음
        }
    }

    /**
     * 포인트로 등급 계산
     */
    private UserGrade calculateGrade(int points) {
        if (points >= DIAMOND_THRESHOLD) return UserGrade.DIAMOND;
        if (points >= PLATINUM_THRESHOLD) return UserGrade.PLATINUM;
        if (points >= GOLD_THRESHOLD) return UserGrade.GOLD;
        if (points >= SILVER_THRESHOLD) return UserGrade.SILVER;
        return UserGrade.BRONZE;
    }

    /**
     * 프로필 조회 또는 생성
     */
    private ProfileEntity getOrCreateProfile(Integer userId) {
        return profileRepository.findByUserEntityUserId(userId)
                .orElseGet(() -> createDefaultProfile(userId));
    }

    /**
     * 기본 프로필 생성
     */
    private ProfileEntity createDefaultProfile(Integer userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        ProfileEntity profile = new ProfileEntity();
        profile.setUserEntity(user);
        profile.setNickname(user.getNickname());
        profile.setPoint(0);
        profile.setGrade(UserGrade.BRONZE);

        return profileRepository.save(profile);
    }

    /**
     * 사용자 포인트 및 등급 조회
     */
    @Transactional(readOnly = true)
    public UserPointInfo getUserPointInfo(Integer userId) {
        ProfileEntity profile = profileRepository.findByUserEntityUserId(userId)
                .orElse(null);

        if (profile == null) {
            return new UserPointInfo(0, UserGrade.BRONZE,
                    SILVER_THRESHOLD, UserGrade.SILVER);
        }

        int currentPoints = profile.getPoint() != null ? profile.getPoint() : 0;
        UserGrade currentGrade = profile.getGrade() != null ? profile.getGrade() : UserGrade.BRONZE;

        // 다음 등급까지 필요한 포인트 계산
        UserGrade nextGrade = getNextGrade(currentGrade);
        int pointsToNext = getPointsToNextGrade(currentPoints, nextGrade);

        return new UserPointInfo(currentPoints, currentGrade, pointsToNext, nextGrade);
    }

    /**
     * 다음 등급 반환
     */
    private UserGrade getNextGrade(UserGrade currentGrade) {
        return switch (currentGrade) {
            case BRONZE -> UserGrade.SILVER;
            case SILVER -> UserGrade.GOLD;
            case GOLD -> UserGrade.PLATINUM;
            case PLATINUM -> UserGrade.DIAMOND;
            case DIAMOND -> UserGrade.DIAMOND; // 최고 등급
        };
    }

    /**
     * 다음 등급까지 필요한 포인트 계산
     */
    private int getPointsToNextGrade(int currentPoints, UserGrade nextGrade) {
        int threshold = switch (nextGrade) {
            case SILVER -> SILVER_THRESHOLD;
            case GOLD -> GOLD_THRESHOLD;
            case PLATINUM -> PLATINUM_THRESHOLD;
            case DIAMOND -> DIAMOND_THRESHOLD;
            default -> 0;
        };

        return Math.max(0, threshold - currentPoints);
    }

    // 포인트 액션 타입
    public enum PointAction {
        POST_CREATE(20, "게시글 작성"),
        REVIEW_CREATE(10, "리뷰 작성"),
        POST_LIKED(5, "게시글 좋아요 받기"),
        REVIEW_LIKED(3, "리뷰 좋아요 받기"),
        DAILY_LOGIN(2, "일일 로그인"),
        FIRST_POST(50, "첫 게시글 작성"),
        FIRST_REVIEW(30, "첫 리뷰 작성");

        private final int points;
        private final String description;

        PointAction(int points, String description) {
            this.points = points;
            this.description = description;
        }

        public int getPoints() {
            return points;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 포인트 정보 DTO
     */
    public static class UserPointInfo {
        private final int currentPoints;
        private final UserGrade currentGrade;
        private final int pointsToNext;
        private final UserGrade nextGrade;

        public UserPointInfo(int currentPoints, UserGrade currentGrade,
                             int pointsToNext, UserGrade nextGrade) {
            this.currentPoints = currentPoints;
            this.currentGrade = currentGrade;
            this.pointsToNext = pointsToNext;
            this.nextGrade = nextGrade;
        }

        // Getters
        public int getCurrentPoints() {
            return currentPoints;
        }

        public UserGrade getCurrentGrade() {
            return currentGrade;
        }

        public int getPointsToNext() {
            return pointsToNext;
        }

        public UserGrade getNextGrade() {
            return nextGrade;
        }
    }
}