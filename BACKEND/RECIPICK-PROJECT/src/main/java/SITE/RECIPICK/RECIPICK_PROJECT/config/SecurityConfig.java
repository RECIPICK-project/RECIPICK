package SITE.RECIPICK.RECIPICK_PROJECT.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 전역 설정 (배포용).
 * <p>
 * 규칙 - Swagger 리소스는 전부 허용 - /users/** : 로그인 필요 - /admin/** : ROLE_ADMIN 권한 필요 - 그 외 요청 : 로그인 필요
 * <p>
 * 비고 - 현재 JWT를 쓰지 않으므로 Basic(임시) + 세션으로 운영 - 컨트롤러의 @PreAuthorize 사용을 위해 @EnableMethodSecurity 활성화
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true) // @PreAuthorize("hasRole('ADMIN')") 등 메소드 보안 활성화
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http

        // 경로별 인가 규칙
        .authorizeHttpRequests(reg -> reg
            // Swagger/OpenAPI 문서 및 정적 리소스는 모두 허용
            .requestMatchers(
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/webjars/**",
                "/css/**", "/js/**", "/img/**", "/", "/error"
            ).permitAll()

            // 마이페이지(사용자 API): 로그인 필요
            .requestMatchers("/users/**", "/me/**").authenticated()

            // 관리자 API: ADMIN 권한 필요 (DB/권한에 ROLE_ 접두사 포함되어 있어야 함)
            .requestMatchers("/admin/**").hasRole("ADMIN")

            // 나머지 전부 로그인 필요
            .anyRequest().authenticated()
        )

        // 임시: Swagger에서 Authorization: Basic 헤더로 테스트 가능
        .httpBasic(Customizer.withDefaults());

    // 세션 전략: 기본값(필요 시 생성) — JWT 미사용 환경에 맞춤
    // .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));

    return http.build();
  }
}
