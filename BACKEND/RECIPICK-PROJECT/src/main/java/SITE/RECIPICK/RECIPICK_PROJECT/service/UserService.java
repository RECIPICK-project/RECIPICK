package SITE.RECIPICK.RECIPICK_PROJECT.service;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.ProfileEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.UserEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.ProfileRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final ProfileRepository profileRepository; // 👈 ProfileRepository 의존성 주입
  private final BCryptPasswordEncoder encoder;
  private final EmailVerificationService emailVerificationService;

  // 공통: 이메일 정규화
  private String norm(String email) {
    return email == null ? null : email.trim().toLowerCase();
  }

  // 회원가입
  @Transactional
  public String signup(String email, String password, String nickname) {
    email = norm(email);

    // ✅ 이메일 인증 확인 (가입 직전)
    if (!emailVerificationService.isEmailVerified(email)) {
      return "이메일 인증이 필요합니다.";
    }

    if (userRepository.findByEmail(email).isPresent()) {
      return "이미 존재하는 이메일입니다.";
    }
    if (userRepository.findByNickname(nickname).isPresent()) {
      return "이미 존재하는 닉네임입니다.";
    }

    UserEntity userEntity =
        UserEntity.builder()
            .email(email)
            .password(encoder.encode(password))
            .nickname(nickname)
            .active(true)
            .stop(0)
            .role("ROLE_USER")
            .build();

    userRepository.save(userEntity);

    // 👈 2. ProfileEntity 생성 및 저장
    ProfileEntity profileEntity = new ProfileEntity();
    profileEntity.setUserEntity(userEntity);
    profileEntity.setNickname(nickname);
    profileEntity.setLatestAt(LocalDateTime.now());
    profileEntity.setUpdatedAt(LocalDateTime.now());
    // grade는 ProfileEntity에 설정된 기본값(BRONZE)으로 자동 설정됨

    profileRepository.save(profileEntity);

    log.info("회원가입 완료: {}", email);
    return "회원가입 성공";
  }

  @Transactional(readOnly = true)
  public boolean isEmailExists(String email) {
    return userRepository.existsByEmail(norm(email));
  }

  @Transactional(readOnly = true)
  public boolean isNicknameExists(String nickname) {
    // 참고: 현재는 users 테이블만 검사하고 있으나, 필요시 profile 테이블도 함께 검사할 수 있습니다.
    // return userRepository.existsByNickname(nickname) || profileRepository.existsByNickname(nickname);
    return userRepository.existsByNickname(nickname);
  }

  // ... (이하 나머지 코드는 동일)
  // 로그인
  public String login(String email, String password) {
    email = norm(email);

    Optional<UserEntity> userOpt = userRepository.findByEmail(email);
    if (userOpt.isEmpty()) {
      return "존재하지 않는 계정입니다.";
    }

    // ✅ 로그인 시에도 이메일 인증 여부 체크 (권장)
    if (!emailVerificationService.isEmailVerified(email)) {
      return "이메일 인증 후 로그인할 수 있습니다.";
    }

    UserEntity userEntity = userOpt.get();
    if (!encoder.matches(password, userEntity.getPassword())) {
      return "비밀번호가 일치하지 않습니다.";
    }

    if (!userEntity.getActive()) {
      return "비활성화된 계정입니다.";
    }

    touchLatest(userEntity.getUserId());

    // ROLE 체크 후 반환
    if ("ROLE_ADMIN".equalsIgnoreCase(userEntity.getRole())) {
      return "관리자 로그인";
    } else {
      return "로그인 성공";
    }
  }

  // 이메일로 유저 조회
  public Optional<UserEntity> getUserByEmail(String email) {
    return userRepository.findByEmail(norm(email));
  }

  // ID로 유저 조회
  public Optional<UserEntity> getUserById(Integer userId) {
    return userRepository.findById(userId);
  }

  // 비밀번호 변경
  @Transactional
  public String changePassword(Integer userId, String oldPassword, String newPassword) {
    Optional<UserEntity> userOpt = userRepository.findById(userId);
    if (userOpt.isEmpty()) {
      return "사용자를 찾을 수 없습니다.";
    }

    UserEntity user = userOpt.get();
    if (!encoder.matches(oldPassword, user.getPassword())) {
      return "기존 비밀번호가 일치하지 않습니다.";
    }

    user.setPassword(encoder.encode(newPassword));
    // save 생략 가능(JPA dirty checking)하지만 명시적으로 두어도 무방
    userRepository.save(user);
    return "비밀번호 변경 성공";
  }

  // 계정 활성화/비활성화 (관리자용)
  @Transactional
  public String setActive(Integer userId, boolean active) {
    Optional<UserEntity> userOpt = userRepository.findById(userId);
    if (userOpt.isEmpty()) {
      return "사용자를 찾을 수 없습니다.";
    }

    UserEntity user = userOpt.get();
    user.setActive(active); // true = 1, false = 0
    userRepository.save(user);

    return active ? "계정 활성화됨" : "계정 정지됨";
  }

  // 전체 유저 리스트 조회 (관리자용)
  public List<UserEntity> getAllUsers() {
    return userRepository.findAll();
  }

  @Transactional
  public void setActiveByEmail(String email, boolean active) {
    email = norm(email);
    Optional<UserEntity> userOpt = userRepository.findByEmail(email);
    if (userOpt.isPresent()) {
      UserEntity user = userOpt.get();
      user.setActive(active);
      userRepository.save(user);
      log.info("사용자 {} active 상태 변경: {}", email, active);
    }
  }

  /**
   * 로그인/활동 시 최신 접속 시각 갱신 (DB를 UTC로 저장한다면 UTC 기준 now)
   */
  @Transactional
  public void touchLatest(Integer userId) {
    userRepository.findById(userId).ifPresent(u -> {
      // DB가 UTC 저장이면 ↓
//      u.setLatestAt(LocalDateTime.now(ZoneOffset.UTC));

      // 만약 DB에 KST로 저장한다면 위 한 줄을 아래로 교체:
      u.setLatestAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
    });
  }

}