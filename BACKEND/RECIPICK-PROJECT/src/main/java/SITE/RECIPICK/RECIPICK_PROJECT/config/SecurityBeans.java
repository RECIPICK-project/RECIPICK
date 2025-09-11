/*
package SITE.RECIPICK.RECIPICK_PROJECT.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

*/
/**
 * SecurityBeans
 * <p>
 * ✅ 역할 - Spring Security에서 사용할 암호화 관련 Bean들을 등록하는 설정 클래스 - 특히 PasswordEncoder를 등록하여 회원가입, 로그인 시
 * 비밀번호를 안전하게 처리한다.
 * <p>
 * ✅ PasswordEncoder - Spring Security는 비밀번호를 평문(plain text)으로 저장/비교하지 않는다. - BCryptPasswordEncoder:
 * BCrypt 해시 함수를 사용해 비밀번호를 암호화. - 매번 다른 salt 값이 자동으로 붙어 동일한 입력이라도 다른 해시값이 생성됨. - rainbow table 같은
 * 공격을 방지할 수 있음.
 * <p>
 * ✅ 사용 예시 - 회원가입 시: 평문 비밀번호를 encoder.encode()로 해싱 후 DB 저장 - 로그인 시: 입력 비밀번호와 DB 저장 해시를
 * encoder.matches()로 비교
 *//*

@Configuration
public class SecurityBeans {

  */
/**
 * PasswordEncoder Bean 등록
 *
 * @return BCryptPasswordEncoder 인스턴스
 * <p>
 * 스프링 컨테이너가 이 메서드 리턴 객체를 빈으로 등록 → 필요한 곳(@Autowired 등)에서 바로 사용 가능
 *//*

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(); // 강력한 해시 함수로 안전하게 비밀번호 암호화
  }
}
*/
