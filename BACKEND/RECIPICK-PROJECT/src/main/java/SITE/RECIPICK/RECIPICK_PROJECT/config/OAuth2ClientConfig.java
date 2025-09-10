package SITE.RECIPICK.RECIPICK_PROJECT.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
public class OAuth2ClientConfig {

  // 1) 환경변수 우선
  @Value("${GOOGLE_CLIENT_ID:}")
  private String envClientId;

  @Value("${GOOGLE_CLIENT_SECRET:}")
  private String envClientSecret;

  // 2) yml에 키가 있으면 폴백
  @Value("${spring.security.oauth2.client.registration.google.client-id:}")
  private String ymlClientId;

  @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
  private String ymlClientSecret;

  @Bean
  public ClientRegistrationRepository clientRegistrationRepository() {
    String clientId = (envClientId != null && !envClientId.isBlank()) ? envClientId : ymlClientId;
    String clientSecret =
        (envClientSecret != null && !envClientSecret.isBlank()) ? envClientSecret : ymlClientSecret;

    if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
      throw new IllegalStateException("Google client-id/secret가 설정되어 있지 않습니다.");
    }

    ClientRegistration google = ClientRegistrations
        .fromIssuerLocation("https://accounts.google.com")
        .registrationId("google")
        .clientId(clientId)
        .clientSecret(clientSecret)
        .scope("openid", "profile", "email")
        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
        .build();

    return new InMemoryClientRegistrationRepository(google);
  }
}
