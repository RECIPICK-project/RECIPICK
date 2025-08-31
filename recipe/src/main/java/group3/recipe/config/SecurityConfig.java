package group3.recipe.config;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

  private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/home.html", "/login.html", "/signup", "/signup.html",
                "/verify-email.html",               //
                "/css/**", "/js/**").permitAll()
            // ✅ 이메일 인증 API는 로그인 없이 접근 가능
            .requestMatchers("/api/auth/email/**").permitAll()

            // ✅ OAuth2 관련 엔드포인트 허용
            .requestMatchers("/oauth2/**", "/login/oauth2/**", "/oauth2/authorization/**").permitAll()

            .requestMatchers("/user-home.html").hasAnyRole("USER","ADMIN")
            .requestMatchers("/admin.html").hasRole("ADMIN")

            .requestMatchers("/api/users/signup", "/api/users/login", "/api/users/check-nickname").permitAll()
            .requestMatchers("/api/users/all", "/api/users/set-active").hasRole("ADMIN")
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/users/**").hasAnyRole("USER","ADMIN")
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/login.html")
            .loginProcessingUrl("/login")
            .usernameParameter("email")          // ⬅️ 폼에서 name="email"이면 꼭 추가
            .passwordParameter("password")       // (옵션) 기본값과 동일
            .successHandler((req, res, auth) -> res.sendRedirect("/user-home.html"))
            .failureHandler((req, res, ex) -> {  // ⬅️ 실패 원인 확인용
              ex.printStackTrace();
              String msg = java.net.URLEncoder.encode(
                  String.valueOf(ex.getMessage()),
                  java.nio.charset.StandardCharsets.UTF_8
              );
              res.sendRedirect("/login.html?error=" + msg);
            })
            .permitAll()
        )

        .oauth2Login(oauth -> oauth
            .loginPage("/login.html")
            .userInfoEndpoint(u -> u.userAuthoritiesMapper(authorities -> {
              java.util.Set<org.springframework.security.core.GrantedAuthority> result = new java.util.HashSet<>();
              result.addAll(authorities);
              result.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"));
              return result;
            }))
            .failureHandler((req, res, ex) -> {
              ex.printStackTrace(); // 콘솔에 상세 스택 출력
              String msg = URLEncoder.encode(
                  ex.getClass().getSimpleName() + ": " + String.valueOf(ex.getMessage()),
                  StandardCharsets.UTF_8
              );
              res.sendRedirect("/login.html?error=" + msg);
            })
            .successHandler(oAuth2LoginSuccessHandler)
        )
        .logout(logout -> logout.logoutSuccessUrl("/login.html").permitAll())   // ✅ (선택) 로그아웃 처리
    ;

    return http.build();
  }

  @Bean
  public BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}