package umc.cockple.demo.global.realtime.publish;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.global.realtime.message.EncodedRealtimeMessage;
import umc.cockple.demo.global.realtime.message.RealtimeMessageEncoder;
import umc.cockple.demo.global.realtime.protocol.RealtimeOutboundEnvelope;
import umc.cockple.demo.global.realtime.session.RealtimeSession;
import umc.cockple.demo.global.realtime.session.RealtimeSessionRegistry;
import umc.cockple.demo.global.realtime.session.WebSocketSessionMessageSender;
import umc.cockple.demo.global.realtime.transport.RealtimeWebSocketEndpoint;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@DisplayName("WebSocketRealtimeMessagePublisher")
class WebSocketRealtimeMessagePublisherTest {

    private RealtimeSessionRegistry sessionRegistry;
    private RealtimeMessageEncoder messageEncoder;
    private WebSocketSessionMessageSender messageSender;
    private WebSocketRealtimeMessagePublisher publisher;

    @BeforeEach
    void setUp() {
        sessionRegistry = mock(RealtimeSessionRegistry.class);
        messageEncoder = mock(RealtimeMessageEncoder.class);
        messageSender = mock(WebSocketSessionMessageSender.class);
        publisher = new WebSocketRealtimeMessagePublisher(
                sessionRegistry,
                messageEncoder,
                messageSender
        );
    }

    @Test
    @DisplayName("회원의 모든 공용 realtime 세션에 같은 표준 envelope를 전송한다")
    void publishesToAllRealtimeSessions() {
        Long memberId = 10L;
        WebSocketSession firstWebSocketSession = session("session-1", true);
        WebSocketSession secondWebSocketSession = session("session-2", true);
        List<RealtimeSession> sessions = List.of(
                realtimeSession(memberId, firstWebSocketSession, 2L),
                realtimeSession(memberId, secondWebSocketSession, 1L)
        );
        EncodedRealtimeMessage encodedMessage = new EncodedRealtimeMessage("encoded");
        ArgumentCaptor<Object> envelopeCaptor = ArgumentCaptor.forClass(Object.class);
        given(sessionRegistry.findOpenSessions(memberId, RealtimeWebSocketEndpoint.SESSION_ENDPOINT))
                .willReturn(sessions);
        given(messageEncoder.encode(envelopeCaptor.capture()))
                .willReturn(Optional.of(encodedMessage));
        given(messageSender.send(firstWebSocketSession, encodedMessage)).willReturn(true);
        given(messageSender.send(secondWebSocketSession, encodedMessage)).willReturn(true);

        RealtimePublishResult result = publisher.publish(
                memberId,
                " chat ",
                "message_received",
                "data"
        );

        RealtimeOutboundEnvelope envelope = (RealtimeOutboundEnvelope) envelopeCaptor.getValue();
        assertThat(envelope.domain()).isEqualTo("CHAT");
        assertThat(envelope.type()).isEqualTo("MESSAGE_RECEIVED");
        assertThat(envelope.requestId()).isNull();
        assertThat(envelope.data()).isEqualTo("data");
        assertThat(result.targetSessionCount()).isEqualTo(2);
        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.failureCount()).isZero();
        assertThat(result.deliveredToAllSessions()).isTrue();
        then(messageSender).should().send(firstWebSocketSession, encodedMessage);
        then(messageSender).should().send(secondWebSocketSession, encodedMessage);
    }

    @Test
    @DisplayName("공용 realtime 세션이 없으면 직렬화하지 않는다")
    void skipsMemberWithoutRealtimeSession() {
        Long memberId = 10L;
        given(sessionRegistry.findOpenSessions(memberId, RealtimeWebSocketEndpoint.SESSION_ENDPOINT))
                .willReturn(List.of());

        RealtimePublishResult result = publisher.publish(memberId, "CHAT", "MESSAGE_RECEIVED", "data");

        assertThat(result).isEqualTo(RealtimePublishResult.noTarget());
        assertThat(result.deliveredToAnySession()).isFalse();
        then(messageEncoder).shouldHaveNoInteractions();
        then(messageSender).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("일부 전송이 실패하면 성공·실패 세션 수를 구분하고 닫힌 세션을 제거한다")
    void reportsPartialFailureAndRemovesClosedSession() {
        Long memberId = 10L;
        WebSocketSession openSession = session("session-1", true);
        WebSocketSession closedSession = session("session-2", false);
        List<RealtimeSession> sessions = List.of(
                realtimeSession(memberId, openSession, 2L),
                realtimeSession(memberId, closedSession, 1L)
        );
        EncodedRealtimeMessage encodedMessage = new EncodedRealtimeMessage("encoded");
        given(sessionRegistry.findOpenSessions(memberId, RealtimeWebSocketEndpoint.SESSION_ENDPOINT))
                .willReturn(sessions);
        given(messageEncoder.encode(any(RealtimeOutboundEnvelope.class)))
                .willReturn(Optional.of(encodedMessage));
        given(messageSender.send(openSession, encodedMessage)).willReturn(true);
        given(messageSender.send(closedSession, encodedMessage)).willReturn(false);

        RealtimePublishResult result = publisher.publish(memberId, "CHAT", "MESSAGE_RECEIVED", "data");

        assertThat(result.targetSessionCount()).isEqualTo(2);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.deliveredToAnySession()).isTrue();
        assertThat(result.deliveredToAllSessions()).isFalse();
        then(sessionRegistry).should().remove(memberId, closedSession);
    }

    @Test
    @DisplayName("표준 envelope 직렬화에 실패하면 대상 세션 수만 포함한 실패 결과를 반환한다")
    void reportsEncodingFailure() {
        Long memberId = 10L;
        WebSocketSession webSocketSession = session("session-1", true);
        RealtimeSession targetSession = realtimeSession(memberId, webSocketSession, 1L);
        given(sessionRegistry.findOpenSessions(memberId, RealtimeWebSocketEndpoint.SESSION_ENDPOINT))
                .willReturn(List.of(targetSession));
        given(messageEncoder.encode(any(RealtimeOutboundEnvelope.class))).willReturn(Optional.empty());

        RealtimePublishResult result = publisher.publish(memberId, "CHAT", "MESSAGE_RECEIVED", "data");

        assertThat(result).isEqualTo(RealtimePublishResult.failed(1));
        then(messageSender).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("publishToSession은 회원의 여러 세션 중 지정한 세션에만 전송한다")
    void publishToSpecificSessionOnly() {
        Long memberId = 10L;
        WebSocketSession targetWebSocketSession = session("session-1", true);
        WebSocketSession otherWebSocketSession = session("session-2", true);
        List<RealtimeSession> sessions = List.of(
                realtimeSession(memberId, targetWebSocketSession, 2L),
                realtimeSession(memberId, otherWebSocketSession, 1L)
        );
        EncodedRealtimeMessage encodedMessage = new EncodedRealtimeMessage("encoded");
        given(sessionRegistry.findOpenSessions(memberId, RealtimeWebSocketEndpoint.SESSION_ENDPOINT))
                .willReturn(sessions);
        given(messageEncoder.encode(any(RealtimeOutboundEnvelope.class)))
                .willReturn(Optional.of(encodedMessage));
        given(messageSender.send(targetWebSocketSession, encodedMessage)).willReturn(true);

        RealtimePublishResult result = publisher.publishToSession(
                memberId, "session-1", "GAME", "BOARD_UPDATED", "data");

        assertThat(result.targetSessionCount()).isEqualTo(1);
        assertThat(result.successCount()).isEqualTo(1);
        then(messageSender).should().send(targetWebSocketSession, encodedMessage);
        then(messageSender).should(org.mockito.Mockito.never()).send(otherWebSocketSession, encodedMessage);
    }

    @Test
    @DisplayName("publishToSession 대상 세션이 열려 있지 않으면 발행하지 않는다")
    void publishToSessionSkipsWhenSessionAbsent() {
        Long memberId = 10L;
        WebSocketSession otherWebSocketSession = session("session-2", true);
        List<RealtimeSession> sessions = List.of(realtimeSession(memberId, otherWebSocketSession, 1L));
        given(sessionRegistry.findOpenSessions(memberId, RealtimeWebSocketEndpoint.SESSION_ENDPOINT))
                .willReturn(sessions);

        RealtimePublishResult result = publisher.publishToSession(
                memberId, "session-1", "GAME", "BOARD_UPDATED", "data");

        assertThat(result).isEqualTo(RealtimePublishResult.noTarget());
        then(messageEncoder).shouldHaveNoInteractions();
        then(messageSender).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("publishToSession은 sessionId가 비어 있으면 거부한다")
    void publishToSessionRejectsBlankSessionId() {
        assertThatThrownBy(() -> publisher.publishToSession(10L, " ", "GAME", "BOARD_UPDATED", "data"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sessionId");

        then(sessionRegistry).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("domain이나 type이 비어 있으면 세션을 조회하기 전에 거부한다")
    void rejectsBlankMessageIdentifier() {
        assertThatThrownBy(() -> publisher.publish(10L, " ", "MESSAGE_RECEIVED", "data"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("domain");
        assertThatThrownBy(() -> publisher.publish(10L, "CHAT", null, "data"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type");

        then(sessionRegistry).shouldHaveNoInteractions();
    }

    private RealtimeSession realtimeSession(
            Long memberId,
            WebSocketSession session,
            long registrationOrder
    ) {
        return new RealtimeSession(
                memberId,
                RealtimeWebSocketEndpoint.SESSION_ENDPOINT,
                session.getId(),
                session,
                registrationOrder
        );
    }

    private WebSocketSession session(String sessionId, boolean open) {
        WebSocketSession session = mock(WebSocketSession.class);
        given(session.getId()).willReturn(sessionId);
        given(session.isOpen()).willReturn(open);
        return session;
    }
}
