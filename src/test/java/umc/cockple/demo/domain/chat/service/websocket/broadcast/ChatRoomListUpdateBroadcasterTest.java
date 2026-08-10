package umc.cockple.demo.domain.chat.service.websocket.broadcast;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO.ChatRoomListUpdate.LastMessageUpdate;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;
import umc.cockple.demo.domain.chat.repository.redis.ChatListSubscriptionStore;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatMessageFanout;
import umc.cockple.demo.global.realtime.message.RealtimeMessageEncoder;
import umc.cockple.demo.global.realtime.message.EncodedRealtimeMessage;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoomListUpdateBroadcaster")
class ChatRoomListUpdateBroadcasterTest {

    @Mock private ChatListSubscriptionStore chatListSubscriptionStore;
    @Mock private RealtimeMessageEncoder messageEncoder;
    @Mock private ChatMessageFanout messageFanout;

    private ChatRoomListUpdateBroadcaster broadcaster;

    @BeforeEach
    void setUp() {
        broadcaster = new ChatRoomListUpdateBroadcaster(chatListSubscriptionStore, messageEncoder, messageFanout);
    }

    @Test
    @DisplayName("채팅방 목록 구독자에게만 개별 목록 업데이트를 전송한다")
    void broadcast_sendsOnlyToChatListSubscribers() {
        // given
        Long chatRoomId = 1L;
        Long subscribedMemberId = 10L;
        Long unsubscribedMemberId = 20L;
        ChatRoomListUpdateData updateData = createUpdateData(3);
        Map<Long, ChatRoomListUpdateData> memberUpdateData = Map.of(
                subscribedMemberId, updateData,
                unsubscribedMemberId, createUpdateData(1)
        );

        given(chatListSubscriptionStore.getChatListSubscribers(chatRoomId))
                .willReturn(Set.of(subscribedMemberId));
        EncodedRealtimeMessage encodedMessage = new EncodedRealtimeMessage("list-update-json");
        given(messageEncoder.encode(org.mockito.ArgumentMatchers.any(WebSocketMessageDTO.ChatRoomListUpdate.class)))
                .willReturn(Optional.of(encodedMessage));
        given(messageFanout.send(
                org.mockito.ArgumentMatchers.eq(subscribedMemberId),
                org.mockito.ArgumentMatchers.eq(encodedMessage),
                org.mockito.ArgumentMatchers.eq(WebSocketMessageType.CHAT_ROOM_LIST_UPDATE),
                org.mockito.ArgumentMatchers.any(WebSocketMessageDTO.ChatRoomListUpdate.class)
        ))
                .willReturn(true);

        // when
        broadcaster.broadcast(chatRoomId, memberUpdateData);

        // then
        ArgumentCaptor<WebSocketMessageDTO.ChatRoomListUpdate> messageCaptor =
                ArgumentCaptor.forClass(WebSocketMessageDTO.ChatRoomListUpdate.class);
        then(messageEncoder).should().encode(messageCaptor.capture());
        then(messageFanout).should().send(
                subscribedMemberId,
                encodedMessage,
                WebSocketMessageType.CHAT_ROOM_LIST_UPDATE,
                messageCaptor.getValue()
        );
        then(messageFanout).shouldHaveNoMoreInteractions();

        WebSocketMessageDTO.ChatRoomListUpdate message = messageCaptor.getValue();
        assertThat(message.type()).isEqualTo(WebSocketMessageType.CHAT_ROOM_LIST_UPDATE);
        assertThat(message.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(message.lastMessage()).isEqualTo(updateData.lastMessage());
        assertThat(message.newUnreadCount()).isEqualTo(updateData.unreadCount());
    }

    private ChatRoomListUpdateData createUpdateData(int unreadCount) {
        LastMessageUpdate lastMessage = LastMessageUpdate.builder()
                .content("hello")
                .timestamp(LocalDateTime.of(2026, 5, 21, 13, 15))
                .messageType("TEXT")
                .build();

        return ChatRoomListUpdateData.builder()
                .lastMessage(lastMessage)
                .unreadCount(unreadCount)
                .build();
    }
}
