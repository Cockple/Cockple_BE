package umc.cockple.demo.global.realtime.transport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.global.realtime.message.EncodedRealtimeMessage;
import umc.cockple.demo.global.realtime.message.RealtimeMessageEncoder;
import umc.cockple.demo.global.realtime.protocol.RealtimeOutboundEnvelope;
import umc.cockple.demo.global.realtime.routing.RealtimeResponder;
import umc.cockple.demo.global.realtime.session.WebSocketSessionMessageSender;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@DisplayName("WebSocketRealtimeResponder")
class WebSocketRealtimeResponderTest {

    @Test
    @DisplayName("도메인과 requestId를 유지한 표준 성공 envelope를 전송한다")
    void sendsSuccessEnvelope() {
        RealtimeMessageEncoder encoder = mock(RealtimeMessageEncoder.class);
        WebSocketSessionMessageSender sender = mock(WebSocketSessionMessageSender.class);
        WebSocketSession session = mock(WebSocketSession.class);
        EncodedRealtimeMessage encodedMessage = new EncodedRealtimeMessage("encoded");
        ArgumentCaptor<Object> envelopeCaptor = ArgumentCaptor.forClass(Object.class);
        given(session.isOpen()).willReturn(true);
        given(encoder.encode(envelopeCaptor.capture())).willReturn(Optional.of(encodedMessage));
        given(sender.send(session, encodedMessage)).willReturn(true);

        RealtimeResponder responder = new WebSocketRealtimeResponderFactory(encoder, sender)
                .create(session, " chat ", "request-1");
        responder.send("MESSAGE_SENT", "data");

        RealtimeOutboundEnvelope envelope = (RealtimeOutboundEnvelope) envelopeCaptor.getValue();
        assertThat(envelope.domain()).isEqualTo("CHAT");
        assertThat(envelope.type()).isEqualTo("MESSAGE_SENT");
        assertThat(envelope.requestId()).isEqualTo("request-1");
        assertThat(envelope.data()).isEqualTo("data");
        assertThat(envelope.error()).isNull();
        then(sender).should().send(session, encodedMessage);
    }

    @Test
    @DisplayName("종료된 세션에는 응답을 직렬화하거나 전송하지 않는다")
    void skipsClosedSession() {
        RealtimeMessageEncoder encoder = mock(RealtimeMessageEncoder.class);
        WebSocketSessionMessageSender sender = mock(WebSocketSessionMessageSender.class);
        WebSocketSession session = mock(WebSocketSession.class);
        given(session.isOpen()).willReturn(false);

        RealtimeResponder responder = new WebSocketRealtimeResponderFactory(encoder, sender)
                .createInfrastructureResponder(session);
        responder.sendError("INVALID_MESSAGE", "잘못된 요청");

        then(encoder).shouldHaveNoInteractions();
        then(sender).shouldHaveNoInteractions();
    }
}
