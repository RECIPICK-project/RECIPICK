package SITE.RECIPICK.RECIPICK_PROJECT;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@SecurityScheme(name = "basicAuth", type = SecuritySchemeType.HTTP, scheme = "basic")
@OpenAPIDefinition(security = {@SecurityRequirement(name = "basicAuth")})
@SpringBootApplication
public class RecipickProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecipickProjectApplication.class, args);
    }
}
