package umc.cockple.demo.global.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 도메인/오리진 관련 웹 설정의 단일 소스(SSOT)
 * 값은 application.yml의 기본값 또는 환경변수(CORS_ALLOWED_ORIGINS, COOKIE_DOMAIN)로 주입
 */
@Getter
@Setter
@Configuration
@Validated
@ConfigurationProperties(prefix = "cockple.web")
public class WebProperties {

    @NotEmpty(message = "cockple.web.allowed-origins must not be empty")
    private List<String> allowedOrigins;

    private String cookieDomain;
}
