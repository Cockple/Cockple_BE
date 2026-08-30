package umc.cockple.demo.global.realtime.transport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.global.realtime.protocol.RealtimeConnectionInfo;
import umc.cockple.demo.global.realtime.routing.RealtimeResponder;
import umc.cockple.demo.global.realtime.session.RealtimeSessionRegistry;
import umc.cockple.demo.global.realtime.session.WebSocketSessionAttributes;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@DisplayName("RealtimeWebSocketHandler")
class RealtimeWebSocketHandlerTest {

    private RealtimeSessionRegistry sessionRegistry;
    private RealtimeWebSocketRequestDispatcher requestDispatcher;
    private WebSocketRealtimeResponderFactory responderFactory;
    private RealtimeResponder responder;
    private WebSocketSession session;
    private Map<String, Object> attributes;
    private RealtimeWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        sessionRegistry = mock(RealtimeSessionRegistry.class);
        requestDispatcher = mock(RealtimeWebSocketRequestDispatcher.class);
        responderFactory = mock(WebSocketRealtimeResponderFactory.class);
        responder = mock(RealtimeResponder.class);
        session = mock(WebSocketSession.class);
        attributes = new HashMap<>();
        given(session.getId()).willReturn("session-1");
        given(session.getAttributes()).willReturn(attributes);
        handler = new RealtimeWebSocketHandler(sessionRegistry, requestDispatcher, responderFactory);
    }

    @Test
    @DisplayName("인증된 연결을 공용 endpoint 세션으로 등록하고 CONNECTED 응답을 보낸다")
    void registersAuthenticatedConnection() throws Exception {
        attributes.put(WebSocketSessionAttributes.MEMBER_ID, 10L);
        attributes.put(WebSocketSessionAttributes.AUTHENTICATED, true);
        given(responderFactory.createInfrastructureResponder(session)).willReturn(responder);
        ArgumentCaptor<Object> dataCaptor = ArgumentCaptor.forClass(Object.class);

        handler.afterConnectionEstablished(session);

        then(sessionRegistry).should().register(
                10L,
                RealtimeWebSocketEndpoint.SESSION_ENDPOINT,
                session
        );
        then(responder).should().send(org.mockito.ArgumentMatchers.eq("CONNECTED"), dataCaptor.capture());
        RealtimeConnectionInfo connectionInfo = (RealtimeConnectionInfo) dataCaptor.getValue();
        assertThat(connectionInfo.memberId()).isEqualTo(10L);
        assertThat(connectionInfo.sessionId()).isEqualTo("session-1");
    }

    @Test
    @DisplayName("인증 정보가 없는 연결은 등록하지 않고 종료한다")
    void closesUnauthenticatedConnection() throws Exception {
        handler.afterConnectionEstablished(session);

        then(session).should().close();
        then(sessionRegistry).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("텍스트 메시지 처리는 공용 dispatcher에 위임한다")
    void delegatesTextMessage() {
        handler.handleTextMessage(session, new TextMessage("payload"));

        then(requestDispatcher).should().dispatch(session, "payload");
    }

    @Test
    @DisplayName("연결 종료 시 해당 공용 세션을 제거한다")
    void removesClosedSession() {
        attributes.put(WebSocketSessionAttributes.MEMBER_ID, 10L);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        then(sessionRegistry).should().remove(10L, session);
    }
}
