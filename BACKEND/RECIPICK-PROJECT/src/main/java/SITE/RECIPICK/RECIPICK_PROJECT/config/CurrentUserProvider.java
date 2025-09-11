package SITE.RECIPICK.RECIPICK_PROJECT.config;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.UserEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

  private final UserRepository userRepository;

  /**
   * 현재 인증된 사용자의 userId를 반환. - httpBasic 기반: Authentication.getName() == email - 없으면
   * IllegalStateException
   */
  public Integer getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
      throw new IllegalStateException("UNAUTHENTICATED");
    }
    String email = auth.getName(); // CustomUserDetailService에서 username으로 email 사용
    Optional<UserEntity> userOpt = userRepository.findByEmail(email);
    return userOpt.orElseThrow(() -> new IllegalStateException("USER_NOT_FOUND")).getUserId();
  }
}
