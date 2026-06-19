package umc.cockple.demo.domain.chat.service.listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.events.ChatRoomListUpdateEvent;
import umc.cockple.demo.domain.chat.service.websocket.ChatRoomListCacheService;
import umc.cockple.demo.domain.chat.service.websocket.broadcast.ChatRoomListUpdateBroadcaster;
import umc.cockple.demo.domain.chat.service.websocket.broadcast.ChatRoomListUpdateData;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoomListUpdateEventListener")
class ChatRoomListUpdateEventListenerTest {

    @Mock private ChatRoomListCacheService chatRoomListCacheService;
    @Mock private ChatRoomListUpdateBroadcaster chatRoomListUpdateBroadcaster;

    private ChatRoomListUpdateEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new ChatRoomListUpdateEventListener(chatRoomListCacheService, chatRoomListUpdateBroadcaster);
    }

    @Test
    @DisplayName("채팅방 목록 업데이트 이벤트는 캐시를 비우고 멤버별 업데이트를 브로드캐스터에 위임한다")
    void handleChatRoomListUpdate_evictsCacheAndDelegatesBroadcast() {
        // given
        Long chatRoomId = 1L;
        LocalDateTime timestamp = LocalDateTime.of(2026, 5, 21, 13, 15);
        ChatRoomListUpdateEvent event = ChatRoomListUpdateEvent.create(
                chatRoomId,
                "hello",
                timestamp,
                "TEXT",
                Map.of(10L, 2)
        );

        // when
        listener.handleChatRoomListUpdate(event);

        // then
        then(chatRoomListCacheService).should().evictLastMessage(chatRoomId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Long, ChatRoomListUpdateData>> updateCaptor =
                ArgumentCaptor.forClass((Class) Map.class);
        then(chatRoomListUpdateBroadcaster).should().broadcast(org.mockito.ArgumentMatchers.eq(chatRoomId), updateCaptor.capture());

        ChatRoomListUpdateData updateData = updateCaptor.getValue().get(10L);
        assertThat(updateData.unreadCount()).isEqualTo(2);
        assertThat(updateData.lastMessage().content()).isEqualTo("hello");
        assertThat(updateData.lastMessage().timestamp()).isEqualTo(timestamp);
        assertThat(updateData.lastMessage().messageType()).isEqualTo("TEXT");
    }
}
