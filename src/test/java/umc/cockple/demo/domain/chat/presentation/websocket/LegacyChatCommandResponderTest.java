package umc.cockple.demo.domain.chat.presentation.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("LegacyChatCommandResponder")
class LegacyChatCommandResponderTest {

    @Mock private WebSocketSession session;
    @Mock private WebSocketResponseSender responseSender;

    private LegacyChatCommandResponder responder;

    @BeforeEach
    void setUp() {
        responder = new LegacyChatCommandResponder(session, responseSender);
    }

    @Test
    @DisplayName("오류를 기존 채팅 응답 형식으로 전달한다")
    void delegatesError() {
        responder.sendError("CHAT_ERROR", "채팅 오류");

        then(responseSender).should().sendErrorMessage(session, "CHAT_ERROR", "채팅 오류");
    }

    @Test
    @DisplayName("채팅방 구독 ACK를 기존 채팅 응답 형식으로 전달한다")
    void delegatesRoomSubscriptionAcknowledgement() {
        responder.acknowledgeRoomSubscription(20L, "SUBSCRIBE");

        then(responseSender).should().sendSubscriptionMessage(session, 20L, "SUBSCRIBE");
    }

    @Test
    @DisplayName("채팅방 목록 구독 ACK를 기존 채팅 응답 형식으로 전달한다")
    void delegatesChatListSubscriptionAcknowledgement() {
        List<Long> chatRoomIds = List.of(20L, 30L);

        responder.acknowledgeChatListSubscription(chatRoomIds, "SUBSCRIBE_CHAT_LIST");

        then(responseSender).should()
                .sendChatListSubscriptionMessage(session, chatRoomIds, "SUBSCRIBE_CHAT_LIST");
    }
}
