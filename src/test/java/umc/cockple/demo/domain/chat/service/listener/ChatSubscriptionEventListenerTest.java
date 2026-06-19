package umc.cockple.demo.domain.chat.service.listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.events.ChatListSubscriptionEvent;
import umc.cockple.demo.domain.chat.events.ChatRoomSubscriptionEvent;
import umc.cockple.demo.domain.chat.repository.redis.ChatListSubscriptionStore;
import umc.cockple.demo.domain.chat.service.websocket.subscription.ChatRoomSubscriptionService;

import java.util.List;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatSubscriptionEventListener")
class ChatSubscriptionEventListenerTest {

    @Mock private ChatRoomSubscriptionService chatRoomSubscriptionService;
    @Mock private ChatListSubscriptionStore chatListSubscriptionStore;

    private ChatSubscriptionEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new ChatSubscriptionEventListener(chatRoomSubscriptionService, chatListSubscriptionStore);
    }

    @Test
    @DisplayName("채팅방 구독 이벤트는 채팅방 구독 서비스에 위임한다")
    void handleChatRoomSubscription_delegatesSubscribe() {
        // given
        ChatRoomSubscriptionEvent event = ChatRoomSubscriptionEvent.subscribe(1L, 10L);

        // when
        listener.handleChatRoomSubscription(event);

        // then
        then(chatRoomSubscriptionService).should().subscribeToChatRoom(1L, 10L);
    }

    @Test
    @DisplayName("채팅방 목록 구독 이벤트는 Redis 구독 저장소에 위임한다")
    void handleChatListSubscription_delegatesSubscribe() {
        // given
        ChatListSubscriptionEvent event = ChatListSubscriptionEvent.subscribe(10L, List.of(1L, 2L));

        // when
        listener.handleChatListSubscription(event);

        // then
        then(chatListSubscriptionStore).should().subscribeToChatList(10L, List.of(1L, 2L));
    }
}
