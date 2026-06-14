package umc.cockple.demo.domain.chat.presentation.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.events.ChatMessageSendEvent;
import umc.cockple.demo.domain.chat.service.ChatValidator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatWebSocketRequestDispatcher")
class ChatWebSocketRequestDispatcherTest {

    @Mock private ChatValidator chatValidator;
    @Mock private WebSocketResponseSender webSocketResponseSender;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private WebSocketSession session;

    private ChatWebSocketRequestDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new ChatWebSocketRequestDispatcher(
                new ObjectMapper(),
                chatValidator,
                webSocketResponseSender,
                eventPublisher
        );
    }

    @Test
    @DisplayName("SEND 요청을 검증한 뒤 메시지 전송 이벤트로 라우팅한다")
    void dispatch_routesSendRequestToChatMessageSendEvent() {
        // given
        Long memberId = 10L;
        Long chatRoomId = 20L;
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("memberId", memberId);
        given(session.getAttributes()).willReturn(attributes);
        given(session.getId()).willReturn("session-1");

        String payload = """
                {"type":"SEND","chatRoomId":20,"content":"hello","images":[]}
                """;

        // when
        dispatcher.dispatch(session, payload);

        // then
        then(chatValidator).should().validateSendRequest(chatRoomId, "hello", List.of(), memberId);

        ArgumentCaptor<ChatMessageSendEvent> eventCaptor = ArgumentCaptor.forClass(ChatMessageSendEvent.class);
        then(eventPublisher).should().publishEvent(eventCaptor.capture());
        ChatMessageSendEvent event = eventCaptor.getValue();
        assertThat(event.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(event.senderId()).isEqualTo(memberId);
        assertThat(event.content()).isEqualTo("hello");
        assertThat(event.files()).isEmpty();

        then(webSocketResponseSender).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("memberId가 없으면 인증 오류 응답을 전송하고 라우팅하지 않는다")
    void dispatch_sendsUnauthorizedError_whenMemberIdMissing() {
        // given
        given(session.getAttributes()).willReturn(new HashMap<>());
        String payload = """
                {"type":"SEND","chatRoomId":20,"content":"hello","images":[]}
                """;

        // when
        dispatcher.dispatch(session, payload);

        // then
        then(webSocketResponseSender).should()
                .sendErrorMessage(session, "UNAUTHORIZED", "인증되지 않은 사용자입니다.");
        then(chatValidator).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }
}
