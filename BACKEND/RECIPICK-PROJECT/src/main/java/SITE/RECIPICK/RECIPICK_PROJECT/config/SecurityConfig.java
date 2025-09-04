package SITE.RECIPICK.RECIPICK_PROJECT.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(reg -> reg
                // Swagger 문서/리소스는 무조건 허용
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // (선택) 마이페이지 API도 지금은 열어두고 테스트
                .requestMatchers("/me/**").permitAll()
                // 나머지 모든 요청도 개발 중엔 허용
                .anyRequest().permitAll()

//            .requestMatchers("/admin/**").hasRole("ADMIN") 실제 서비스에서는 어드민만 되게
        )
        .httpBasic(Customizer.withDefaults()); // Swagger 테스트 편의용
    return http.build();
  }
}
