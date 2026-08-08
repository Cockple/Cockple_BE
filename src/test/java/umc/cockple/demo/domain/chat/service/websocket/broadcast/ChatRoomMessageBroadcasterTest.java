package umc.cockple.demo.domain.chat.service.websocket.broadcast;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.MessageType;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;
import umc.cockple.demo.global.realtime.message.RealtimeMessageEncoder;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatMessageSender;
import umc.cockple.demo.global.realtime.message.EncodedRealtimeMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoomMessageBroadcaster")
class ChatRoomMessageBroadcasterTest {

    @Mock private RealtimeMessageEncoder messageEncoder;
    @Mock private ChatMessageSender messageSender;

    private ChatRoomMessageBroadcaster broadcaster;

    @BeforeEach
    void setUp() {
        broadcaster = new ChatRoomMessageBroadcaster(messageEncoder, messageSender);
    }

    @Test
    @DisplayName("제외 멤버를 건너뛰고 나머지 구독자에게 인코딩된 메시지를 전송한다")
    void broadcast_sendsEncodedMessageExceptExcludedMember() {
        // given
        Long chatRoomId = 1L;
        Long excludedMemberId = 10L;
        WebSocketMessageDTO.MessageResponse message = createMessage(chatRoomId);
        EncodedRealtimeMessage encodedMessage = new EncodedRealtimeMessage("message-json");
        given(messageEncoder.encode(message)).willReturn(Optional.of(encodedMessage));
        given(messageSender.send(20L, encodedMessage)).willReturn(true);
        given(messageSender.send(30L, encodedMessage)).willReturn(false);

        // when
        broadcaster.broadcast(chatRoomId, message, List.of(excludedMemberId, 20L, 30L), excludedMemberId);

        // then
        then(messageEncoder).should().encode(message);
        then(messageSender).should().send(20L, encodedMessage);
        then(messageSender).should().send(30L, encodedMessage);
        then(messageSender).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("구독자가 없으면 메시지를 직렬화하지 않는다")
    void broadcast_doesNotSerializeWhenSubscribersEmpty() {
        // given
        Long chatRoomId = 1L;
        WebSocketMessageDTO.MessageResponse message = createMessage(chatRoomId);

        // when
        broadcaster.broadcast(chatRoomId, message, List.of(), null);

        // then
        then(messageEncoder).shouldHaveNoInteractions();
        then(messageSender).shouldHaveNoInteractions();
    }

    private WebSocketMessageDTO.MessageResponse createMessage(Long chatRoomId) {
        return WebSocketMessageDTO.MessageResponse.builder()
                .type(WebSocketMessageType.SEND)
                .chatRoomId(chatRoomId)
                .messageId(100L)
                .content("hello")
                .messageType(MessageType.TEXT)
                .images(List.of())
                .senderId(10L)
                .senderName("sender")
                .timestamp(LocalDateTime.of(2026, 5, 21, 13, 15))
                .unreadCount(1)
                .build();
    }
}
