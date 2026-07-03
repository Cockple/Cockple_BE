package umc.cockple.demo.domain.chat.presentation.websocket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import umc.cockple.demo.global.logging.MdcLoggingFilter;
import umc.cockple.demo.global.security.filter.JwtAuthenticationFilter;
import umc.cockple.demo.global.jwt.domain.JwtTokenProvider;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWTWebSocketAuthInterceptor")
class JWTWebSocketAuthInterceptorTest {

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private ServerHttpRequest request;
    @Mock private ServerHttpResponse response;
    @Mock private WebSocketHandler wsHandler;

    private JWTWebSocketAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new JWTWebSocketAuthInterceptor(jwtTokenProvider);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("handshake 인증 성공 시 세션 attributes에 memberId를 저장하고 MDC를 복구한다")
    void beforeHandshakeStoresMemberIdAndRestoresMdc() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        given(request.getURI()).willReturn(URI.create("/ws/chats?token=access-token"));
        given(jwtTokenProvider.validateToken("access-token")).willReturn(true);
        given(jwtTokenProvider.getUserId("access-token")).willReturn(10L);
        MDC.put(MdcLoggingFilter.REQUEST_ID, "request-1");

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isTrue();
        assertThat(attributes).containsEntry(JwtAuthenticationFilter.MEMBER_ID, 10L);
        assertThat(attributes).containsEntry("authenticated", true);
        assertThat(MDC.get(MdcLoggingFilter.REQUEST_ID)).isEqualTo("request-1");
        assertThat(MDC.get(JwtAuthenticationFilter.MEMBER_ID)).isNull();
        assertThat(MDC.get(WebSocketMdcSupport.WS_SESSION_ID)).isNull();
    }
}
