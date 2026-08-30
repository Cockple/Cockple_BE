package umc.cockple.demo.global.realtime.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.global.realtime.config.RealtimeWebSocketProperties;
import umc.cockple.demo.global.realtime.protocol.RealtimeInboundEnvelope;
import umc.cockple.demo.global.realtime.routing.RealtimeConnectionContext;
import umc.cockple.demo.global.realtime.routing.RealtimeMessageRouter;
import umc.cockple.demo.global.realtime.routing.RealtimeResponder;
import umc.cockple.demo.global.realtime.session.WebSocketSessionAttributes;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@DisplayName("RealtimeWebSocketRequestDispatcher")
class RealtimeWebSocketRequestDispatcherTest {

    private RealtimeMessageRouter messageRouter;
    private WebSocketRealtimeResponderFactory responderFactory;
    private RealtimeWebSocketProperties properties;
    private WebSocketSession session;
    private RealtimeResponder fallbackResponder;
    private RealtimeResponder requestResponder;
    private RealtimeWebSocketRequestDispatcher dispatcher;
    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        messageRouter = mock(RealtimeMessageRouter.class);
        responderFactory = mock(WebSocketRealtimeResponderFactory.class);
        properties = new RealtimeWebSocketProperties();
        session = mock(WebSocketSession.class);
        fallbackResponder = mock(RealtimeResponder.class);
        requestResponder = mock(RealtimeResponder.class);
        attributes = new HashMap<>();

        given(session.getId()).willReturn("session-1");
        given(session.getAttributes()).willReturn(attributes);
        given(responderFactory.createInfrastructureResponder(session)).willReturn(fallbackResponder);

        dispatcher = new RealtimeWebSocketRequestDispatcher(
                new ObjectMapper(),
                messageRouter,
                responderFactory,
                properties
        );
    }

    @Test
    @DisplayName("인증된 요청 envelope를 연결 정보와 함께 router에 전달한다")
    void dispatchesAuthenticatedRequest() {
        attributes.put(WebSocketSessionAttributes.MEMBER_ID, 10L);
        given(responderFactory.create(session, "chat", "request-1")).willReturn(requestResponder);
        String payload = "{\"version\":1,\"domain\":\"chat\",\"action\":\"send\",\"requestId\":\"request-1\",\"payload\":{\"message\":\"hi\"}}";
        ArgumentCaptor<RealtimeConnectionContext> contextCaptor =
                ArgumentCaptor.forClass(RealtimeConnectionContext.class);
        ArgumentCaptor<RealtimeInboundEnvelope> envelopeCaptor =
                ArgumentCaptor.forClass(RealtimeInboundEnvelope.class);

        dispatcher.dispatch(session, payload);

        then(messageRouter).should().route(
                contextCaptor.capture(),
                envelopeCaptor.capture(),
                org.mockito.ArgumentMatchers.same(requestResponder)
        );
        assertThat(contextCaptor.getValue().memberId()).isEqualTo(10L);
        assertThat(contextCaptor.getValue().sessionId()).isEqualTo("session-1");
        assertThat(envelopeCaptor.getValue().action()).isEqualTo("send");
        assertThat(envelopeCaptor.getValue().payload().get("message").asText()).isEqualTo("hi");
    }

    @Test
    @DisplayName("JSON으로 파싱할 수 없는 요청은 공용 INVALID_MESSAGE 오류로 응답한다")
    void rejectsMalformedJson() {
        dispatcher.dispatch(session, "not-json");

        then(fallbackResponder).should().sendError(
                "INVALID_MESSAGE",
                "실시간 요청 형식이 올바르지 않습니다."
        );
        then(messageRouter).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("JSON null 요청도 INVALID_MESSAGE 오류로 응답한다")
    void rejectsNullEnvelope() {
        dispatcher.dispatch(session, "null");

        then(fallbackResponder).should().sendError(
                "INVALID_MESSAGE",
                "실시간 요청 형식이 올바르지 않습니다."
        );
        then(messageRouter).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("설정된 최대 길이를 넘은 요청은 파싱 전에 거부한다")
    void rejectsOversizedPayload() {
        properties.setMaxPayloadLength(3);

        dispatcher.dispatch(session, "1234");

        then(fallbackResponder).should().sendError(
                "MESSAGE_TOO_LARGE",
                "실시간 요청 크기가 허용 범위를 초과했습니다."
        );
        then(messageRouter).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("인증 회원 정보가 없는 요청은 requestId를 유지한 UNAUTHORIZED 오류로 응답한다")
    void rejectsUnauthenticatedRequest() {
        given(responderFactory.create(session, "CHAT", "request-1")).willReturn(requestResponder);
        String payload = "{\"version\":1,\"domain\":\"CHAT\",\"action\":\"SEND\",\"requestId\":\"request-1\"}";

        dispatcher.dispatch(session, payload);

        then(requestResponder).should().sendError("UNAUTHORIZED", "인증되지 않은 사용자입니다.");
        then(messageRouter).shouldHaveNoInteractions();
    }
}
