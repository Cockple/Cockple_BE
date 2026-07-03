package umc.cockple.demo.global.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class MdcLoggingFilterTest {

    private final MdcLoggingFilter filter = new MdcLoggingFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("X-Request-Id가 있으면 요청 MDC에 재사용하고 종료 후 정리한다")
    void reuseRequestIdAndClearMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/exercises");
        request.addHeader("X-Request-Id", "request-123");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> requestId = new AtomicReference<>();
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> uri = new AtomicReference<>();
        AtomicReference<String> clientIp = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            requestId.set(MDC.get(MdcLoggingFilter.REQUEST_ID));
            method.set(MDC.get(MdcLoggingFilter.METHOD));
            uri.set(MDC.get(MdcLoggingFilter.URI));
            clientIp.set(MDC.get(MdcLoggingFilter.CLIENT_IP));
        });

        assertThat(requestId.get()).isEqualTo("request-123");
        assertThat(method.get()).isEqualTo("POST");
        assertThat(uri.get()).isEqualTo("/api/exercises");
        assertThat(clientIp.get()).isEqualTo("203.0.113.10");
        assertThat(MDC.get(MdcLoggingFilter.REQUEST_ID)).isNull();
        assertThat(MDC.get(MdcLoggingFilter.METHOD)).isNull();
        assertThat(MDC.get(MdcLoggingFilter.URI)).isNull();
        assertThat(MDC.get(MdcLoggingFilter.CLIENT_IP)).isNull();
    }

    @Test
    @DisplayName("X-Request-Id가 없으면 UUID를 생성한다")
    void generateRequestIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members/me");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> requestId = new AtomicReference<>();
        AtomicReference<String> clientIp = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            requestId.set(MDC.get(MdcLoggingFilter.REQUEST_ID));
            clientIp.set(MDC.get(MdcLoggingFilter.CLIENT_IP));
        });

        assertThatCode(() -> UUID.fromString(requestId.get())).doesNotThrowAnyException();
        assertThat(clientIp.get()).isEqualTo("127.0.0.1");
        assertThat(MDC.get(MdcLoggingFilter.REQUEST_ID)).isNull();
        assertThat(MDC.get(MdcLoggingFilter.CLIENT_IP)).isNull();
    }
}
