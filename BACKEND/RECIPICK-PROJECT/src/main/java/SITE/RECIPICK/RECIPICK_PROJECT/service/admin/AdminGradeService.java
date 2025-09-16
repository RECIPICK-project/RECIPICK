package SITE.RECIPICK.RECIPICK_PROJECT.service.admin;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.GradeUpdateRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.UserGrade;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminGradeService {

    private final ProfileRepository profileRepo;

    @Transactional
    public void updateUserGrade(Integer userId, GradeUpdateRequest req) {
        var pr =
                profileRepo
                        .findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("PROFILE_NOT_FOUND"));

        var g = req.getGrade();
        if (g == null || g.isBlank()) {
            throw new IllegalArgumentException("GRADE_REQUIRED");
        }

        UserGrade newGrade;
        try {
            newGrade = UserGrade.valueOf(g.trim().toUpperCase()); // 입력값 소문자 대응
        } catch (Exception e) {
            throw new IllegalArgumentException("INVALID_GRADE"); // BRONZE/SILVER/GOLD 외 입력 시
        }

        pr.changeGrade(newGrade);
        pr.setUpdatedAt(LocalDateTime.now()); // 필요시 수정 시간 갱신
    }
}
