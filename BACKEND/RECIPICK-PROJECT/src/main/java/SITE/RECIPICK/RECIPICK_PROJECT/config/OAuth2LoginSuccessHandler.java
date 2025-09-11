package SITE.RECIPICK.RECIPICK_PROJECT.config;


import SITE.RECIPICK.RECIPICK_PROJECT.entity.UserEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.UserRepository;
import SITE.RECIPICK.RECIPICK_PROJECT.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

  private final UserRepository userRepository;
  private final EmailService emailService;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication) throws IOException {

    if (!(authentication instanceof OAuth2AuthenticationToken)) {
      log.error("Unexpected authentication type: {}", authentication.getClass());
      response.sendRedirect("/login.html?error=unexpected_auth");
      return;
    }

    OAuth2User principal = (OAuth2User) authentication.getPrincipal();
    Map<String, Object> attr = principal.getAttributes();
    log.debug("OAuth2 principal class={}, attrs={}", principal.getClass().getName(), attr.keySet());

    String email = asString(attr.get("email"));
    String name  = firstNonEmpty(asString(attr.get("name")),
        asString(attr.get("given_name")),
        "사용자");

    // OIDC면 email_verified, fullName 보정
    Boolean emailVerified = null;
    if (principal instanceof OidcUser oidc) {
      emailVerified = oidc.getEmailVerified();
      if (email == null) email = oidc.getEmail();
      if (oidc.getFullName() != null && !oidc.getFullName().isBlank()) name = oidc.getFullName();
    } else {
      Object ev = attr.get("email_verified");
      if (ev instanceof Boolean b) emailVerified = b;
      else if (ev instanceof String s) emailVerified = Boolean.valueOf(s);
    }

    if (email == null || email.isBlank()) {
      response.sendRedirect("/login.html?error=email_not_provided");
      return;
    }

    // ===== 업서트 (람다 X) =====
    Optional<UserEntity> existing = userRepository.findByEmail(email);
    UserEntity user;
    if (existing.isPresent()) {
      user = existing.get();
    } else {
      String nickname = uniqueNickname(name);
      user = UserEntity.builder()
          .email(email)
          .nickname(nickname)
          .password(null)           // 소셜은 비번 없음
          .role("ROLE_USER")
          .provider("GOOGLE")
          .active(false)            // 이메일 인증 전
          .stop(0)
          .build();
      user = userRepository.save(user);
    }

    // provider 최신화
    if (!"GOOGLE".equalsIgnoreCase(user.getProvider())) {
      user.setProvider("GOOGLE");
      userRepository.save(user);
    }

    // 구글이 이메일 이미 검증한 경우 → 바로 활성화 후 메인
    if (Boolean.TRUE.equals(emailVerified)) {
      if (!Boolean.TRUE.equals(user.getActive())) {
        user.setActive(true);
        userRepository.save(user);
      }
      response.sendRedirect("/main.html");
      return;
    }

    // 그 외 → 인증코드 메일 전송 후 인증 페이지
    try {
      emailService.sendVerificationCode(email);
    } catch (Exception e) {
      log.error("Failed to send verification email: {}", e.getMessage(), e);
      response.sendRedirect("/login.html?error=mail_failed");
      return;
    }

    String q = URLEncoder.encode(email, StandardCharsets.UTF_8);
    response.sendRedirect("/email-verification.html?email=" + q);
  }

  private static String asString(Object o) { return o == null ? null : String.valueOf(o); }

  private static String firstNonEmpty(String... xs) {
    for (String x : xs) if (x != null && !x.isBlank()) return x;
    return null;
  }

  private String uniqueNickname(String base) {
    String safe = (base == null || base.isBlank()) ? "user" : base.trim();
    if (safe.length() > 50) safe = safe.substring(0, 50);
    String candidate = safe;
    int i = 1;
    while (userRepository.findByNickname(candidate).isPresent()) {
      candidate = safe + "_" + i++;
      if (candidate.length() > 50) candidate = candidate.substring(0, 50);
    }
    return candidate;
  }
}