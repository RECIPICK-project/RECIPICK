package group3.recipe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

  @Bean
  public JavaMailSender javaMailSender() {
    JavaMailSenderImpl s = new JavaMailSenderImpl();
    s.setHost("smtp.gmail.com");
    s.setPort(587);

    // 환경변수에서 읽어오기 (권장)
    String username = System.getenv("GMAIL_USERNAME");
    String appPassword = System.getenv("GMAIL_APP_PASSWORD");

    // 환경변수 없으면 yml 값으로 대체하고 싶다면 주석 해제 후 사용:
    // (스프링 자동설정으로 가는 게 더 깔끔합니다)
    // @Autowired Environment env; -> 필드 주입 추가 후
    // username = username != null ? username : env.getProperty("spring.mail.username");
    // appPassword = appPassword != null ? appPassword : env.getProperty("spring.mail.password");

    s.setUsername(username);
    s.setPassword(appPassword);

    Properties p = s.getJavaMailProperties();
    p.put("mail.transport.protocol", "smtp");
    p.put("mail.smtp.auth", "true");
    p.put("mail.smtp.starttls.enable", "true");
    p.put("mail.smtp.starttls.required", "true");
    p.put("mail.debug", "true");
    return s;
  }
}