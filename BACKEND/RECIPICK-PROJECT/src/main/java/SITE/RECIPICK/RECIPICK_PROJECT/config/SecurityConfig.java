package SITE.RECIPICK.RECIPICK_PROJECT.config;

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
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
                // 에러 페이지 허용 (무한 리다이렉트 방지)
                .requestMatchers("/error").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/admin/import/**").permitAll()
                .requestMatchers("/api/main/**").permitAll()

                // 정적 리소스 및 페이지 허용 (pages 폴더 경로 반영)
                .requestMatchers("/", "/pages/**",
                    "/css/**", "/js/**", "/images/**", "/api/posts/**").permitAll()

//            // 정적 리소스 및 페이지 허용 (pages 폴더 경로 반영)
//            .requestMatchers("/", "/pages/home.html", "/pages/login.html",
//                "/pages/signup.html", "/pages/email-verification.html",
//                "/css/**", "/js/**", "/images/**").permitAll()

                // 이메일 인증 API
                .requestMatchers("/api/auth/email/**").permitAll()

                // 중복 확인 API (비로그인 허용)
                .requestMatchers("/api/users/check-email", "/api/users/check-nickname").permitAll()

                // 회원가입/로그인 API (비로그인 허용)
                .requestMatchers("/api/users/signup", "/api/users/login").permitAll()

                // OAuth2 관련 엔드포인트 허용
                .requestMatchers("/oauth2/**", "/login/oauth2/**", "/oauth2/authorization/**")
                .permitAll()

                // 페이지 접근 권한 (pages 폴더 경로 반영)
//            .requestMatchers("/pages/main.html").hasAnyRole("USER","ADMIN")
//            .requestMatchers("/pages/admin.html").hasRole("ADMIN")

                // 관리용 API
                .requestMatchers("/api/users/all", "/api/users/set-active").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // 리뷰 목록 조회는 비로그인 허용, 나머지는 로그인 필요
                .requestMatchers("/api/reviews/post/*/stats", "/api/reviews/post/*").permitAll()
                .requestMatchers("/api/reviews/**").hasAnyRole("USER", "ADMIN")

                // 나머지 사용자 API
                .requestMatchers("/api/users/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().hasAnyRole("USER", "ADMIN")
        )
        .formLogin(form -> form
            .loginPage("/pages/login.html")  // pages 폴더 경로로 수정
            .loginProcessingUrl("/login")
            .usernameParameter("email")
            .passwordParameter("password")
            .successHandler(
                (req, res, auth) -> res.sendRedirect("/pages/main.html"))  // pages 폴더 경로로 수정
            .failureHandler((req, res, ex) -> {
              ex.printStackTrace();
              String msg = URLEncoder.encode(
                  String.valueOf(ex.getMessage()),
                  StandardCharsets.UTF_8
              );
              res.sendRedirect("/pages/login.html?error=" + msg);  // pages 폴더 경로로 수정
            })
            .permitAll()
        )

        .oauth2Login(oauth -> oauth
            .loginPage("/pages/login.html")  // pages 폴더 경로로 수정
            .userInfoEndpoint(u -> u.userAuthoritiesMapper(authorities -> {
              java.util.Set<org.springframework.security.core.GrantedAuthority> result = new java.util.HashSet<>();
              result.addAll(authorities);
              result.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                  "ROLE_USER"));
              return result;
            }))
            .failureHandler((req, res, ex) -> {
              ex.printStackTrace();
              String msg = URLEncoder.encode(
                  ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                  StandardCharsets.UTF_8
              );
              res.sendRedirect("/pages/login.html?error=" + msg);  // pages 폴더 경로로 수정
            })
            .successHandler(oAuth2LoginSuccessHandler)
        )
        .logout(
            logout -> logout.logoutSuccessUrl("/pages/login.html").permitAll())  // pages 폴더 경로로 수정
    ;

    return http.build();
  }

  // CORS 설정 (8080 포트 추가)
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration c = new CorsConfiguration();
    c.setAllowedOriginPatterns(List.of(
        "http://127.0.0.1:5500",
        "http://localhost:5500",
        "http://localhost:8080",  // 추가
        "http://localhost:9999"   // 추가 (혹시 필요할 경우)
    ));
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    c.setAllowedHeaders(List.of("*"));
    c.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", c);
    return src;
  }

  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
