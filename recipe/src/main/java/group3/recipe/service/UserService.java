package group3.recipe.service;

import group3.recipe.entity.UserEntity;
import group3.recipe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor

public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;
    private final EmailVerificationService emailVerificationService;

    // 회원가입
    @Transactional
    public String signup(String email, String password, String nickname) {
        // 이메일 인증 확인
        boolean isVerified = emailVerificationService.isEmailVerified(email);
        if (!isVerified) {
            return "이메일 인증이 필요합니다.";
        }

        if (userRepository.findByEmail(email).isPresent()) {
            return "이미 존재하는 이메일입니다.";
        }
        if (userRepository.findByNickname(nickname).isPresent()) {
            return "이미 존재하는 닉네임입니다.";
        }

        // 회원가입
        UserEntity userEntity = UserEntity.builder()
            .email(email)
            .password(encoder.encode(password))
            .nickname(nickname)
            .active(true)
            .stop(0)
            .role("ROLE_USER")
            .build();

        userRepository.save(userEntity);
        log.info("회원가입 완료: {}", email);
        return "회원가입 성공";
    }

    public boolean isNicknameExists(String nickname) {
        return userRepository.findByNickname(nickname).isPresent();
    }

    // 로그인
    public String login(String email, String password) {
        Optional<UserEntity> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) return "존재하지 않는 계정입니다.";

        UserEntity userEntity = userOpt.get();
        if (!encoder.matches(password, userEntity.getPassword())) {
            return "비밀번호가 일치하지 않습니다.";
        }

        if (!userEntity.getActive()) {
            return "비활성화된 계정입니다.";
        }

        // ROLE 체크 후 반환
        if(userEntity.getRole().equalsIgnoreCase("ROLE_ADMIN")) {
            return "관리자 로그인";
        } else {
            return "로그인 성공";
        }
    }

    // 이메일로 유저 조회
    public Optional<UserEntity> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // ID로 유저 조회
    public Optional<UserEntity> getUserById(Integer userId) {
        return userRepository.findById(userId);
    }

    // 비밀번호 변경
    public String changePassword(Integer userId, String oldPassword, String newPassword) {
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if(userOpt.isEmpty()) return "사용자를 찾을 수 없습니다.";

        UserEntity user = userOpt.get();
        if(!encoder.matches(oldPassword, user.getPassword())) {
            return "기존 비밀번호가 일치하지 않습니다.";
        }

        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);
        return "비밀번호 변경 성공";


    }

    // 계정 활성화/비활성화 (관리자용)
    public String setActive(Integer userId, boolean active) {
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if(userOpt.isEmpty()) return "사용자를 찾을 수 없습니다.";

        UserEntity user = userOpt.get();
        user.setActive(active);  // true = 1, false = 0
        userRepository.save(user);

        return active ? "계정 활성화됨" : "계정 정지됨";
    }

    // 전체 유저 리스트 조회 (관리자용)
    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    public void setActiveByEmail(String email, boolean active) {
        Optional<UserEntity> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            user.setActive(active);
            userRepository.save(user);
            log.info("사용자 {} active 상태 변경: {}", email, active);
        }
    }

}
