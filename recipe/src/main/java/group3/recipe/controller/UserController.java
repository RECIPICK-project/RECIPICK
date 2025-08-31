package group3.recipe.controller;

import group3.recipe.dto.UserSignupDto;
import group3.recipe.entity.UserEntity;
import group3.recipe.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User API", description = "회원 관련 API")
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원가입", description = "이메일과 비밀번호로 신규 회원을 등록합니다")
    @PostMapping("/signup")
    public String signup(@RequestBody Map<String, String> user) {
        String email = user.get("email");
        String password = user.get("password");
        String nickname = user.get("nickname");
        return userService.signup(email, password, nickname);
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다")
    @PostMapping("/login")
    public String login(@RequestBody Map<String, String> user) {
        String email = user.get("email");
        String password = user.get("password");
        return userService.login(email, password);
    }

    @Operation(summary = "전체 유저 조회", description = "관리자용: 모든 회원 조회")
    @GetMapping("/all")
    public List<UserEntity> getAllUsers() {
        return userService.getAllUsers();
    }

    @Operation(summary = "계정 활성화/비활성화", description = "관리자용: 유저 계정을 활성화하거나 비활성화합니다")
    @PostMapping("/set-active")
    public String setActive(@RequestParam Integer userId,
        @RequestParam boolean active) {
        return userService.setActive(userId, active);
    }

    @GetMapping("/user-info")
    public Map<String, String> getUserInfo(HttpServletRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal().equals("anonymousUser")) {
            return Map.of("username", "", "role", "");
        }

        // username
        String username = auth.getName();

        // role
        String role = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .findFirst()
            .orElse("");

        return Map.of("username", username, "role", role);
    }

    @GetMapping("/check-nickname")
    public Map<String, Boolean> checkNickname(@RequestParam String nickname) {
        log.info("닉네임 체크 요청: {}", nickname);
        boolean exists = userService.isNicknameExists(nickname);
        log.info("닉네임 존재 여부: {}", exists);
        return Map.of("exists", exists);
    }
}