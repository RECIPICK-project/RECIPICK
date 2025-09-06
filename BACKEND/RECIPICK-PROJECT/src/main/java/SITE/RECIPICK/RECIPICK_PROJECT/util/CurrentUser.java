package SITE.RECIPICK.RECIPICK_PROJECT.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class CurrentUser {

  public static Integer currentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      throw new IllegalStateException("인증된 사용자가 없습니다.");
    }
    // UserDetailsService에서 username=email로 세팅했으니 여기서 userId를 찾으려면 DB 조회 필요
    // 지금은 예시로 email을 리턴
    return Integer.valueOf(auth.getName());
  }
}
