//package SITE.RECIPICK.RECIPICK_PROJECT.controller;
//
//import SITE.RECIPICK.RECIPICK_PROJECT.entity.UserEntity;
//import SITE.RECIPICK.RECIPICK_PROJECT.service.PointService;
//import SITE.RECIPICK.RECIPICK_PROJECT.service.UserService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.Map;
//import java.util.Optional;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/points")
//@RequiredArgsConstructor
//@Tag(name = "Point API", description = "포인트 및 등급 관련 API")
//public class PointController {
//
//    private final PointService pointService;
//    private final UserService userService;
//
//    @Operation(summary = "내 포인트 및 등급 정보 조회", description = "현재 사용자의 포인트, 등급, 다음 등급까지 필요한 포인트를 조회합니다")
//    @GetMapping("/my-info")
//    public ResponseEntity<?> getMyPointInfo() {
//        try {
//            Integer currentUserId = getCurrentUserId();
//            if (currentUserId == null) {
//                return ResponseEntity.status(401)
//                        .body(Map.of("error", "인증이 필요합니다."));
//            }
//
//            PointService.UserPointInfo pointInfo = pointService.getUserPointInfo(currentUserId);
//
//            Map<String, Object> response = Map.of(
//                    "currentPoints", pointInfo.getCurrentPoints(),
//                    "currentGrade", pointInfo.getCurrentGrade().name(),
//                    "currentGradeDisplay", getGradeDisplayName(pointInfo.getCurrentGrade()),
//                    "nextGrade", pointInfo.getNextGrade().name(),
//                    "nextGradeDisplay", getGradeDisplayName(pointInfo.getNextGrade()),
//                    "pointsToNext", pointInfo.getPointsToNext(),
//                    "isMaxGrade", pointInfo.getCurrentGrade() == pointInfo.getNextGrade()
//            );
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("포인트 정보 조회 중 오류", e);
//            return ResponseEntity.internalServerError()
//                    .body(Map.of("error", "서버 오류가 발생했습니다."));
//        }
//    }
//
//    @Operation(summary = "포인트 등급 시스템 정보", description = "등급별 필요 포인트 정보를 조회합니다")
//    @GetMapping("/grade-system")
//    public ResponseEntity<Map<String, Object>> getGradeSystemInfo() {
//        Map<String, Object> gradeSystem = Map.of(
//                "grades", Map.of(
//                        "BRONZE", Map.of("points", 0, "display", "BRONZE", "color", "#CD7F32"),
//                        "SILVER", Map.of("points", 100, "display", "SILVER", "color", "#C0C0C0"),
//                        "GOLD", Map.of("points", 300, "display", "GOLD", "color", "#FFD700"),
//                        "PLATINUM", Map.of("points", 800, "display", "PLATINUM", "color", "#E5E4E2"),
//                        "DIAMOND", Map.of("points", 2000, "display", "DIAMOND", "color", "#B9F2FF")
//                ),
//                "pointActions", Map.of(
//                        "POST_CREATE", Map.of("points", 20, "description", "게시글 작성"),
//                        "REVIEW_CREATE", Map.of("points", 10, "description", "리뷰 작성"),
//                        "POST_LIKED", Map.of("points", 5, "description", "게시글 좋아요 받기"),
//                        "FIRST_POST", Map.of("points", 50, "description", "첫 게시글 작성 보너스"),
//                        "FIRST_REVIEW", Map.of("points", 30, "description", "첫 리뷰 작성 보너스"),
//                        "DAILY_LOGIN", Map.of("points", 2, "description", "일일 로그인")
//                )
//        );
//
//        return ResponseEntity.ok(gradeSystem);
//    }
//
//    @Operation(summary = "특정 사용자 포인트 정보 조회", description = "관리자용: 특정 사용자의 포인트 정보를 조회합니다")
//    @GetMapping("/user/{userId}")
//    public ResponseEntity<?> getUserPointInfo(@PathVariable Integer userId) {
//        try {
//            // 관리자 권한 체크 (선택사항)
//            Integer currentUserId = getCurrentUserId();
//            if (currentUserId == null) {
//                return ResponseEntity.status(401)
//                        .body(Map.of("error", "인증이 필요합니다."));
//            }
//
//            PointService.UserPointInfo pointInfo = pointService.getUserPointInfo(userId);
//
//            Map<String, Object> response = Map.of(
//                    "userId", userId,
//                    "currentPoints", pointInfo.getCurrentPoints(),
//                    "currentGrade", pointInfo.getCurrentGrade().name(),
//                    "pointsToNext", pointInfo.getPointsToNext(),
//                    "nextGrade", pointInfo.getNextGrade().name()
//            );
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("사용자 포인트 정보 조회 중 오류 - userId: {}", userId, e);
//            return ResponseEntity.internalServerError()
//                    .body(Map.of("error", "서버 오류가 발생했습니다."));
//        }
//    }
//
//    /**
//     * 등급 한글 표시명 반환
//     */
//    private String getGradeDisplayName(SITE.RECIPICK.RECIPICK_PROJECT.entity.UserGrade grade) {
//        return switch (grade) {
//            case BRONZE -> "브론즈";
//            case SILVER -> "실버";
//            case GOLD -> "골드";
//            case PLATINUM -> "플래티넘";
//            case DIAMOND -> "다이아몬드";
//        };
//    }
//
//    /**
//     * 현재 로그인한 사용자 ID 조회
//     */
//    private Integer getCurrentUserId() {
//        try {
//            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//
//            if (authentication == null || !authentication.isAuthenticated()
//                    || "anonymousUser".equals(authentication.getPrincipal())) {
//                return null;
//            }
//
//            String email = authentication.getName();
//            Optional<UserEntity> userOpt = userService.getUserByEmail(email);
//
//            return userOpt.map(UserEntity::getUserId).orElse(null);
//
//        } catch (Exception e) {
//            log.error("현재 사용자 ID 조회 중 오류", e);
//            return null;
//        }
//    }
//}