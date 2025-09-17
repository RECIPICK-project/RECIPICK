package SITE.RECIPICK.RECIPICK_PROJECT.service;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.MyProfileResponse;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.NicknameUpdateRequest;
import SITE.RECIPICK.RECIPICK_PROJECT.dto.PostDto;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.PostRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.ProfileRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.ReviewRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.UserRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.util.PostMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 📌 마이페이지 관련 서비스 - 프로필 조회, 닉네임 변경, 좋아요 목록 조회
 */
@Service
@RequiredArgsConstructor
public class MyPageService {

  private final ProfileRepository profileRepo;
  private final PostRepository postRepo;
  private final ReviewRepository reviewRepo;
  private final UserRepository userRepo;

  @Transactional(readOnly = true)
  public MyProfileResponse getMyProfile(Integer me) {
    var pr = profileRepo.findById(me)
        .orElseThrow(() -> new IllegalArgumentException("PROFILE_NOT_FOUND"));

    long myRecipeCount = postRepo.countPublishedByAuthor(me);
    long totalLikesOnMyPosts = postRepo.sumLikesOnUsersPublished(me);

    long reviewCount = reviewRepo.countByUserUserId(me);
    long commentCount;
    try {
      // COMMENT 리포지토리 분리 전 임시 방어
      commentCount = reviewRepo.countByUserUserId(me);
    } catch (Exception e) {
      commentCount = 0;
    }

    return new MyProfileResponse(
        pr.getNickname(),
        pr.getGrade().name(),
        pr.getProfileImg(),
        myRecipeCount,
        totalLikesOnMyPosts,
        reviewCount + commentCount
    );
  }

  @Transactional
  public void changeNickname(Integer me, NicknameUpdateRequest req) {
    String raw = req.getNewNickname();
    if (raw == null || raw.trim().isEmpty()) {
      throw new IllegalArgumentException("닉네임은 공백일 수 없습니다.");
    }
    String newNickname = raw.trim();
    if (newNickname.length() > 50) {
      throw new IllegalArgumentException("닉네임이 너무 깁니다.");
    }

    var pr = profileRepo.findById(me)
        .orElseThrow(() -> new IllegalArgumentException("프로필을 찾을 수 없습니다."));

    if (newNickname.equals(pr.getNickname())) {
      return;
    }

    var lastUpdated = pr.getUpdatedAt();
    if (lastUpdated != null && lastUpdated.isAfter(java.time.LocalDateTime.now().minusDays(7))) {
      throw new IllegalStateException("닉네임은 일주일에 한 번만 변경할 수 있습니다.");
    }

    if (profileRepo.existsByNickname(newNickname)) {
      throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
    }

    pr.setNickname(newNickname);
    pr.setUpdatedAt(java.time.LocalDateTime.now());
    profileRepo.save(pr);

    var user = userRepo.findById(me)
        .orElseThrow(() -> new IllegalArgumentException("프로필을 찾을 수 없습니다."));
    user.setNickname(newNickname);
    userRepo.save(user);
  }

  @Transactional(readOnly = true)
  public List<PostDto> getMyLikedPosts(Integer me, int offset, int limit) {
    var pageable = PageRequest.of(offset / Math.max(1, limit), Math.max(1, limit));
    return postRepo.findLikedPosts(me, pageable)
        .stream()
        .map(PostMapper::toDto)
        .toList();
  }
}
