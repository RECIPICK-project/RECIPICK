package group3.recipe.config;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

  private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
            // ✅ 프론트(라이브 서버 5500)에서 API 호출 허용
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/home.html", "/login.html", "/signup", "/signup.html",
                "/email-verification.html",               //
                "/css/**", "/js/**").permitAll()
            // ✅ 이메일 인증 API는 로그인 없이 접근 가능
            .requestMatchers("/api/auth/email/**").permitAll()

            // ✅ 중복 확인 API (비로그인 허용)
            .requestMatchers("/api/users/check-email", "/api/users/check-nickname").permitAll()

            // ✅ 회원가입/로그인 API (비로그인 허용)
            .requestMatchers("/api/users/signup", "/api/users/login").permitAll()

            // ✅ OAuth2 관련 엔드포인트 허용
            .requestMatchers("/oauth2/**", "/login/oauth2/**", "/oauth2/authorization/**").permitAll()

            // ✅페이지 접근 권한
            .requestMatchers("/main.html").hasAnyRole("USER","ADMIN")
            .requestMatchers("/admin.html").hasRole("ADMIN")

            // ✅관리용 API
            .requestMatchers("/api/users/all", "/api/users/set-active").hasRole("ADMIN")
            .requestMatchers("/api/admin/**").hasRole("ADMIN")

            // ✅나머지 사용자 API
            .requestMatchers("/api/users/**").hasAnyRole("USER","ADMIN")
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/login.html")
            .loginProcessingUrl("/login")
            .usernameParameter("email")          // ⬅️ 폼에서 name="email"이면 꼭 추가
            .passwordParameter("password")       // (옵션) 기본값과 동일
            .successHandler((req, res, auth) -> res.sendRedirect("/main.html"))
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
  // ✅ 프론트(라이브 서버) 오리진 허용: 주소/포트 필요에 맞게 추가
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration c = new CorsConfiguration();
    c.setAllowedOriginPatterns(List.of(
        "http://127.0.0.1:5500",
        "http://localhost:5500"
        // 필요하면 추가: "http://localhost:3000", "http://localhost:5173" 등
    ));
    c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
    c.setAllowedHeaders(List.of("*"));
    // 쿠키/세션을 쓸 때만 true (JWT 헤더 방식이면 false여도 됨)
    c.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", c);
    return src;
  }

  @Bean
  public BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}