package umc.cockple.demo.domain.chat.presentation.websocket.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.service.websocket.session.EncodedChatMessage;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketMessageSender")
class WebSocketMessageSenderTest {

    @Mock private WebSocketSessionRegistry sessionRegistry;
    @Mock private WebSocketSessionMessageSender sessionMessageSender;
    @Mock private WebSocketSession session;

    private WebSocketMessageSender messageSender;

    @BeforeEach
    void setUp() {
        messageSender = new WebSocketMessageSender(sessionRegistry, sessionMessageSender);
    }

    @Test
    @DisplayName("인코딩된 메시지를 열린 세션에 전송한다")
    void send_sendsEncodedMessageToOpenSession() {
        // given
        Long memberId = 10L;
        given(sessionRegistry.findOpenSession(memberId)).willReturn(Optional.of(session));
        EncodedChatMessage message = new EncodedChatMessage("{\"type\":\"UNREAD_STATUS_UPDATE\"}");
        given(sessionMessageSender.send(session, message)).willReturn(true);

        // when
        boolean sent = messageSender.send(memberId, message);

        // then
        assertThat(sent).isTrue();
        ArgumentCaptor<EncodedChatMessage> messageCaptor = ArgumentCaptor.forClass(EncodedChatMessage.class);
        then(sessionMessageSender).should().send(org.mockito.ArgumentMatchers.eq(session), messageCaptor.capture());
        assertThat(messageCaptor.getValue().payload()).contains("\"type\":\"UNREAD_STATUS_UPDATE\"");
    }

    @Test
    @DisplayName("대상 세션이 없으면 실패만 반환한다")
    void send_returnsFalseWhenSessionDoesNotExist() {
        // given
        Long memberId = 10L;
        given(sessionRegistry.findOpenSession(memberId)).willReturn(Optional.empty());

        // when
        boolean sent = messageSender.send(memberId, new EncodedChatMessage("{}"));

        // then
        assertThat(sent).isFalse();
        then(sessionRegistry).should().findOpenSession(memberId);
        then(sessionRegistry).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("전송 중 예외가 발생하면 세션 저장소에서 제거하고 실패를 반환한다")
    void send_removesMemberWhenSendFails() throws Exception {
        // given
        Long memberId = 10L;
        given(sessionRegistry.findOpenSession(memberId)).willReturn(Optional.of(session));
        EncodedChatMessage message = new EncodedChatMessage("{}");
        given(sessionMessageSender.send(session, message)).willReturn(false);

        // when
        boolean sent = messageSender.send(memberId, message);

        // then
        assertThat(sent).isFalse();
        then(sessionRegistry).should().remove(memberId, session);
    }
}
