/*
package SITE.RECIPICK.RECIPICK_PROJECT.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

*/
/**
 * 인증 컨텍스트에서 로그인 사용자 ID를 꺼내는 헬퍼. - 현재는 username(email)을 Integer로 파싱해 쓰지 않고, CustomUserPrincipal 등에
 * userId를 넣어두고 getName()으로 돌려받는 전략을 가정. - 프로젝트 상황에 맞게 변환 로직만 손보면 됨.
 *//*

public final class AuthUtil {

  private AuthUtil() {
  }

  public static Integer getLoginUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getName() == null) {
      throw new IllegalStateException("UNAUTHENTICATED");
    }
    // 예시) CustomUserPrincipal.getName()을 userId 문자열로 세팅한 경우
    try {
      return Integer.valueOf(auth.getName());
    } catch (NumberFormatException e) {
      // 만약 auth.getName()이 이메일이라면, UserRepository로 조회해 ID를 가져오는 방식으로 교체 필요
      throw new IllegalStateException("INVALID_AUTH_PRINCIPAL: name=" + auth.getName());
    }
  }
}
*/
