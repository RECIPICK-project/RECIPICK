package SITE.RECIPICK.RECIPICK_PROJECT.config;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.UserEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.UserRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.service.EmailService;
import SITE.RECIPICK.RECIPICK_PROJECT.service.ProfileInitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final ProfileInitService profileInitService;
    private final EmailService emailService;

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String firstNonEmpty(String... xs) {
        for (String x : xs) {
            if (x != null && !x.isBlank()) {
                return x;
            }
        }
        return null;
    }

    @Transactional
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException {

        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            log.error("Unexpected authentication type: {}", authentication.getClass());
            response.sendRedirect("/pages/login.html?error=unexpected_auth");
            return;
        }

        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attr = principal.getAttributes();
        log.debug("OAuth2 principal class={}, attrs={}", principal.getClass().getName(), attr.keySet());

        String email = asString(attr.get("email"));
        String name = firstNonEmpty(
                asString(attr.get("name")),
                asString(attr.get("given_name")),
                "사용자"
        );

        Boolean emailVerified = null;
        if (principal instanceof OidcUser oidc) {
            emailVerified = oidc.getEmailVerified();
            if (email == null) {
                email = oidc.getEmail();
            }
            if (oidc.getFullName() != null && !oidc.getFullName().isBlank()) {
                name = oidc.getFullName();
            }
        } else {
            Object ev = attr.get("email_verified");
            if (ev instanceof Boolean b) {
                emailVerified = b;
            } else if (ev instanceof String s) {
                emailVerified = Boolean.valueOf(s);
            }
        }

        if (email == null || email.isBlank()) {
            response.sendRedirect("/pages/login.html?error=email_not_provided");
            return;
        }

        // ===== 업서트 =====
        Optional<UserEntity> existing = userRepository.findByEmail(email);
        UserEntity user;
        boolean isNewUser = false;

        if (existing.isPresent()) {
            user = existing.get();
        } else {
            isNewUser = true;
            String nickname = uniqueNickname(name);
            user = UserEntity.builder()
                    .email(email)
                    .nickname(nickname)
                    .password(null)
                    .role("ROLE_USER")
                    .provider("GOOGLE")
                    .active(false)
                    .stop(0)
                    .build();
            user = userRepository.save(user);
        }

        // provider 최신화
        if (!"GOOGLE".equalsIgnoreCase(user.getProvider())) {
            user.setProvider("GOOGLE");
            userRepository.save(user);
        }

        // ProfileEntity 생성/확인
        profileInitService.ensureProfileExists(user.getUserId());

        // 구글이 이메일 이미 검증한 경우 → 바로 활성화 후 메인
        if (Boolean.TRUE.equals(emailVerified)) {
            if (!Boolean.TRUE.equals(user.getActive())) {
                user.setActive(true);
                userRepository.save(user);
            }

            // SecurityContext에 새로운 인증 객체 저장
            // OAuth2 인증을 UsernamePasswordAuthenticationToken으로 변환
            UsernamePasswordAuthenticationToken newAuth =
                    new UsernamePasswordAuthenticationToken(
                            email,  // principal
                            null,   // credentials
                            List.of(new SimpleGrantedAuthority(user.getRole()))
                    );

            SecurityContextHolder.getContext().setAuthentication(newAuth);

            // 세션에 명시적으로 저장
            request.getSession().setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext()
            );

            log.info("OAuth2 로그인 성공 - email: {}, role: {}, userId: {}",
                    email, user.getRole(), user.getUserId());

            response.sendRedirect("/pages/main.html");
            return;
        }

        // 그 외 → 인증코드 메일 전송 후 인증 페이지
        try {
            emailService.sendVerificationCode(email);
        } catch (Exception e) {
            log.error("Failed to send verification email: {}", e.getMessage(), e);
            response.sendRedirect("/pages/login.html?error=mail_failed");
            return;
        }

        String q = URLEncoder.encode(email, StandardCharsets.UTF_8);
        response.sendRedirect("/pages/email-verification.html?email=" + q);
    }

    private String uniqueNickname(String base) {
        String safe = (base == null || base.isBlank()) ? "user" : base.trim();
        if (safe.length() > 50) {
            safe = safe.substring(0, 50);
        }
        String candidate = safe;
        int i = 1;
        while (userRepository.findByNickname(candidate).isPresent()) {
            candidate = safe + "_" + i++;
            if (candidate.length() > 50) {
                candidate = candidate.substring(0, 50);
            }
        }
        return candidate;
    }
}