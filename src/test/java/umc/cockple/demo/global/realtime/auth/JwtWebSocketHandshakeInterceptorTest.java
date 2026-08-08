package umc.cockple.demo.global.realtime.auth;

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
import umc.cockple.demo.global.jwt.domain.JwtTokenProvider;
import umc.cockple.demo.global.logging.MdcLoggingFilter;
import umc.cockple.demo.global.realtime.logging.WebSocketMdcSupport;
import umc.cockple.demo.global.realtime.session.WebSocketSessionAttributes;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtWebSocketHandshakeInterceptor")
class JwtWebSocketHandshakeInterceptorTest {

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private ServerHttpRequest request;
    @Mock private ServerHttpResponse response;
    @Mock private WebSocketHandler wsHandler;

    private JwtWebSocketHandshakeInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new JwtWebSocketHandshakeInterceptor(jwtTokenProvider);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("handshake 인증 성공 시 세션 attributes에 memberId를 저장하고 MDC를 복구한다")
    void beforeHandshakeStoresMemberIdAndRestoresMdc() {
        Map<String, Object> attributes = new HashMap<>();
        given(request.getURI()).willReturn(URI.create("/ws/chats?token=access-token"));
        given(jwtTokenProvider.validateToken("access-token")).willReturn(true);
        given(jwtTokenProvider.getUserId("access-token")).willReturn(10L);
        MDC.put(MdcLoggingFilter.REQUEST_ID, "request-1");

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isTrue();
        assertThat(attributes).containsEntry(WebSocketSessionAttributes.MEMBER_ID, 10L);
        assertThat(attributes).containsEntry(WebSocketSessionAttributes.AUTHENTICATED, true);
        assertThat(MDC.get(MdcLoggingFilter.REQUEST_ID)).isEqualTo("request-1");
        assertThat(MDC.get(WebSocketSessionAttributes.MEMBER_ID)).isNull();
        assertThat(MDC.get(WebSocketMdcSupport.WS_SESSION_ID)).isNull();
    }

    @Test
    @DisplayName("token 쿼리 파라미터가 없으면 handshake를 거부한다")
    void beforeHandshakeRejectsRequestWithoutToken() {
        Map<String, Object> attributes = new HashMap<>();
        given(request.getURI()).willReturn(URI.create("/ws/chats"));

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isFalse();
        assertThat(attributes).isEmpty();
        then(jwtTokenProvider).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("유효하지 않은 token이면 handshake를 거부한다")
    void beforeHandshakeRejectsInvalidToken() {
        Map<String, Object> attributes = new HashMap<>();
        given(request.getURI()).willReturn(URI.create("/ws/chats?token=invalid-token"));
        given(jwtTokenProvider.validateToken("invalid-token")).willReturn(false);

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isFalse();
        assertThat(attributes).isEmpty();
        then(jwtTokenProvider).should().validateToken("invalid-token");
        then(jwtTokenProvider).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("다른 쿼리 파라미터 사이의 token도 추출한다")
    void beforeHandshakeExtractsTokenAmongOtherQueryParameters() {
        Map<String, Object> attributes = new HashMap<>();
        given(request.getURI()).willReturn(URI.create("/ws/chats?transport=websocket&token=access-token&v=1"));
        given(jwtTokenProvider.validateToken("access-token")).willReturn(true);
        given(jwtTokenProvider.getUserId("access-token")).willReturn(10L);

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isTrue();
        assertThat(attributes)
                .containsEntry(WebSocketSessionAttributes.MEMBER_ID, 10L)
                .containsEntry(WebSocketSessionAttributes.AUTHENTICATED, true);
    }
}
