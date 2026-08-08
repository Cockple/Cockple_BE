package umc.cockple.demo.global.realtime.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.global.logging.MdcLoggingFilter;
import umc.cockple.demo.global.realtime.session.WebSocketSessionAttributes;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("WebSocketMdcSupport")
class WebSocketMdcSupportTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("세션 scope 동안 memberId와 wsSessionId를 MDC에 넣고 종료 후 이전 MDC를 복구한다")
    void openSessionMdcAndRestorePreviousContext() {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(WebSocketSessionAttributes.MEMBER_ID, 10L);
        given(session.getId()).willReturn("session-1");
        given(session.getAttributes()).willReturn(attributes);
        MDC.put(MdcLoggingFilter.REQUEST_ID, "request-1");

        try (WebSocketMdcSupport.MdcScope ignored = WebSocketMdcSupport.open(session)) {
            assertThat(MDC.get(WebSocketSessionAttributes.MEMBER_ID)).isEqualTo("10");
            assertThat(MDC.get(WebSocketMdcSupport.WS_SESSION_ID)).isEqualTo("session-1");
            assertThat(MDC.get(MdcLoggingFilter.REQUEST_ID)).isEqualTo("request-1");
        }

        assertThat(MDC.get(MdcLoggingFilter.REQUEST_ID)).isEqualTo("request-1");
        assertThat(MDC.get(WebSocketSessionAttributes.MEMBER_ID)).isNull();
        assertThat(MDC.get(WebSocketMdcSupport.WS_SESSION_ID)).isNull();
    }

    @Test
    @DisplayName("memberId scope는 wsSessionId를 비우고 종료 후 이전 MDC를 복구한다")
    void openMemberMdcAndClearWebSocketSessionId() {
        MDC.put(MdcLoggingFilter.REQUEST_ID, "request-1");
        MDC.put(WebSocketMdcSupport.WS_SESSION_ID, "stale-session");

        try (WebSocketMdcSupport.MdcScope ignored = WebSocketMdcSupport.open(20L)) {
            assertThat(MDC.get(WebSocketSessionAttributes.MEMBER_ID)).isEqualTo("20");
            assertThat(MDC.get(WebSocketMdcSupport.WS_SESSION_ID)).isNull();
            assertThat(MDC.get(MdcLoggingFilter.REQUEST_ID)).isEqualTo("request-1");
        }

        assertThat(MDC.get(MdcLoggingFilter.REQUEST_ID)).isEqualTo("request-1");
        assertThat(MDC.get(WebSocketMdcSupport.WS_SESSION_ID)).isEqualTo("stale-session");
        assertThat(MDC.get(WebSocketSessionAttributes.MEMBER_ID)).isNull();
    }
}
