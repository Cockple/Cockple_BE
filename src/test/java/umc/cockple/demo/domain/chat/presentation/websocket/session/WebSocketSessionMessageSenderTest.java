package umc.cockple.demo.domain.chat.presentation.websocket.session;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.service.websocket.session.EncodedChatMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

@DisplayName("WebSocketSessionMessageSender")
class WebSocketSessionMessageSenderTest {

    private final WebSocketSessionMessageSender sessionMessageSender = new WebSocketSessionMessageSender();

    @Test
    @DisplayName("인코딩된 payload를 WebSocket TextMessage로 전송한다")
    void send_sendsTextMessagePayload() throws Exception {
        // given
        WebSocketSession session = mock(WebSocketSession.class);
        EncodedChatMessage message = new EncodedChatMessage("{\"type\":\"CONNECT\"}");

        // when
        boolean sent = sessionMessageSender.send(session, message);

        // then
        assertThat(sent).isTrue();
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        then(session).should().sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getPayload()).isEqualTo(message.payload());
    }

    @Test
    @DisplayName("전송 실패 시 false를 반환한다")
    void send_returnsFalseWhenSendFails() throws Exception {
        // given
        WebSocketSession session = mock(WebSocketSession.class);
        given(session.getId()).willReturn("session-1");
        willThrow(new RuntimeException("socket closed"))
                .given(session)
                .sendMessage(org.mockito.ArgumentMatchers.any(TextMessage.class));

        // when
        boolean sent = sessionMessageSender.send(session, new EncodedChatMessage("{}"));

        // then
        assertThat(sent).isFalse();
    }
}
