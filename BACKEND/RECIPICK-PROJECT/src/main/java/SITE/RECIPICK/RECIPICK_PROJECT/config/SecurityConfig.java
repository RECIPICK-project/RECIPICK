package SITE.RECIPICK.RECIPICK_PROJECT.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

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

                        // 개발 도구 (필요시)
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // 메인 페이지 관련 API (비로그인 허용)
                        .requestMatchers("/api/main/**").permitAll()

                        // 특정 페이지만 비로그인 허용
                        .requestMatchers("/pages/main.html", "/pages/login.html", "/pages/signup.html").permitAll()
                        .requestMatchers("/pages/email-verification.html").permitAll() // 이메일 인증 페이지

                        // 정적 리소스 허용
                        .requestMatchers("/css/**", "/js/**", "/image/**", "/favicon.ico").permitAll()

                        // 이메일 인증 API
                        .requestMatchers("/api/auth/email/**").permitAll()

                        // 중복 확인 API (비로그인 허용)
                        .requestMatchers("/api/users/check-email", "/api/users/check-nickname").permitAll()

                        // 회원가입/로그인 API (비로그인 허용)
                        .requestMatchers("/api/users/signup", "/api/users/login").permitAll()

                        // OAuth2 관련 엔드포인트 허용
                        .requestMatchers("/oauth2/**", "/login/oauth2/**", "/oauth2/authorization/**").permitAll()

                        // 로그인 체크 API (인증 필요)
                        .requestMatchers("/api/users/review_check").authenticated()

                        // 관리용 API
                        .requestMatchers("/api/users/all", "/api/users/set-active").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 나머지 모든 요청은 로그인 필요
                        .anyRequest().hasAnyRole("USER", "ADMIN")
                )
                .formLogin(form -> form
                        .loginPage("/pages/login.html")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler((req, res, auth) -> {
                            // 일반 로그인 성공 시에도 세션 저장 확인
                            req.getSession().setAttribute(
                                    org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                                    org.springframework.security.core.context.SecurityContextHolder.getContext()
                            );
                            res.sendRedirect("/pages/main.html");
                        })
                        .failureHandler((req, res, ex) -> {
                            ex.printStackTrace();
                            String msg = URLEncoder.encode(
                                    String.valueOf(ex.getMessage()),
                                    StandardCharsets.UTF_8
                            );
                            res.sendRedirect("/pages/login.html?error=" + msg);
                        })
                        .permitAll()
                )
                .oauth2Login(oauth -> oauth
                        .loginPage("/pages/login.html")
                        .userInfoEndpoint(u -> u.userAuthoritiesMapper(authorities -> {
                            java.util.Set<org.springframework.security.core.GrantedAuthority> result = new java.util.HashSet<>();
                            result.addAll(authorities);
                            result.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"));
                            return result;
                        }))
                        .failureHandler((req, res, ex) -> {
                            ex.printStackTrace();
                            String msg = URLEncoder.encode(
                                    ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                                    StandardCharsets.UTF_8
                            );
                            res.sendRedirect("/pages/login.html?error=" + msg);
                        })
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/pages/login.html")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                );

        return http.build();
    }

    // CORS 설정 (8080 포트 추가)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOriginPatterns(List.of(
                "http://127.0.0.1:5500",
                "http://localhost:5500",
                "http://localhost:8080",
                "http://localhost:9999"
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