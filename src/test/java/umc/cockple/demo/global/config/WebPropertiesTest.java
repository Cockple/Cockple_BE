package umc.cockple.demo.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebProperties 바인딩 검증.
 * 도메인 값을 하드코딩에서 설정 바인딩으로 옮긴 뒤에도, 운영과 "동일한" origin 목록/쿠키 도메인이
 * 실제로 그대로 풀리는지(no-op) 배포 없이 결정적으로 확인한다.
 */
class WebPropertiesTest {

    private WebProperties bind(Map<String, Object> props) {
        Binder binder = new Binder(new MapConfigurationPropertySource(props));
        return binder.bind("cockple.web", WebProperties.class).get();
    }

    @Test
    @DisplayName("application.yml 기본값(콤마 구분)이 운영과 동일한 origin 목록으로 바인딩된다")
    void bindsCommaSeparatedOriginsExactlyAsBefore() {
        // application.yml의 CORS_ALLOWED_ORIGINS 기본값과 동일한 문자열
        String defaultOrigins =
                "http://localhost:5173,https://cockple.store,https://www.cockple.store,"
                        + "https://staging.cockple.store,https://cockple-fe.vercel.app";

        WebProperties props = bind(Map.of(
                "cockple.web.allowed-origins", defaultOrigins,
                "cockple.web.cookie-domain", ".cockple.store"
        ));

        // 리팩토링 전 하드코딩되어 있던 값과 정확히 일치해야 한다(순수 no-op)
        assertThat(props.getAllowedOrigins()).containsExactly(
                "http://localhost:5173",
                "https://cockple.store",
                "https://www.cockple.store",
                "https://staging.cockple.store",
                "https://cockple-fe.vercel.app"
        );
        assertThat(props.getCookieDomain()).isEqualTo(".cockple.store");
    }

    @Test
    @DisplayName("cookie-domain이 비면 null/blank로 바인딩되어 host-only 쿠키로 동작한다")
    void blankCookieDomainMeansHostOnly() {
        WebProperties props = bind(Map.of(
                "cockple.web.allowed-origins", "http://localhost:5173",
                "cockple.web.cookie-domain", ""
        ));

        String cookieDomain = props.getCookieDomain();
        assertThat(cookieDomain == null || cookieDomain.isBlank()).isTrue();
    }

    @Test
    @DisplayName("env로 새 도메인을 주입하면 코드 변경 없이 목록이 교체된다")
    void envOverrideReplacesOrigins() {
        WebProperties props = bind(Map.of(
                "cockple.web.allowed-origins", "https://new-domain.com,https://api.new-domain.com"
        ));

        assertThat(props.getAllowedOrigins())
                .containsExactly("https://new-domain.com", "https://api.new-domain.com");
    }
}
