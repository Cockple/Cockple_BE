package umc.cockple.demo.domain.chat.service.websocket.subscription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.repository.redis.ChatRoomSubscriptionStore;
import umc.cockple.demo.domain.chat.service.websocket.UnreadCountUpdate;
import umc.cockple.demo.domain.chat.service.websocket.broadcast.UnreadCountUpdateBroadcaster;
import umc.cockple.demo.domain.chat.service.websocket.subscription.support.ActiveChatRoomSubscriberReader;
import umc.cockple.demo.domain.chat.service.websocket.subscription.support.SubscribeReadStatusService;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoomSubscriptionService")
class ChatRoomSubscriptionServiceTest {

    @Mock private SubscribeReadStatusService subscribeReadStatusService;
    @Mock private ChatRoomSubscriptionStore chatRoomSubscriptionStore;
    @Mock private UnreadCountUpdateBroadcaster unreadCountUpdateBroadcaster;
    @Mock private ActiveChatRoomSubscriberReader activeChatRoomSubscriberReader;

    private ChatRoomSubscriptionService chatRoomSubscriptionService;

    @BeforeEach
    void setUp() {
        chatRoomSubscriptionService = new ChatRoomSubscriptionService(
                subscribeReadStatusService,
                chatRoomSubscriptionStore,
                unreadCountUpdateBroadcaster,
                activeChatRoomSubscriberReader
        );
    }

    @Test
    @DisplayName("구독 시 읽음 처리 결과가 있으면 안읽은 수 업데이트 broadcaster에 위임한다")
    void subscribeToChatRoom_delegatesUnreadCountUpdatesToBroadcaster() {
        // given
        Long chatRoomId = 1L;
        Long memberId = 10L;
        List<Long> activeSubscribers = List.of(memberId, 20L);
        List<UnreadCountUpdate> updates =
                List.of(new UnreadCountUpdate(100L, 1));

        given(subscribeReadStatusService.markUnreadMessagesAsReadOnSubscribe(chatRoomId, memberId))
                .willReturn(updates);
        given(activeChatRoomSubscriberReader.findActiveSubscribers(chatRoomId)).willReturn(activeSubscribers);

        // when
        chatRoomSubscriptionService.subscribeToChatRoom(chatRoomId, memberId);

        // then
        then(chatRoomSubscriptionStore).should().addSubscriber(chatRoomId, memberId);
        then(unreadCountUpdateBroadcaster).should()
                .broadcast(chatRoomId, updates, activeSubscribers, memberId);
    }

    @Test
    @DisplayName("구독 시 읽음 처리 결과가 없으면 안읽은 수 업데이트를 보내지 않는다")
    void subscribeToChatRoom_doesNotBroadcastWhenNoUnreadUpdates() {
        // given
        Long chatRoomId = 1L;
        Long memberId = 10L;
        given(subscribeReadStatusService.markUnreadMessagesAsReadOnSubscribe(chatRoomId, memberId))
                .willReturn(List.of());

        // when
        chatRoomSubscriptionService.subscribeToChatRoom(chatRoomId, memberId);

        // then
        then(chatRoomSubscriptionStore).should().addSubscriber(chatRoomId, memberId);
        then(activeChatRoomSubscriberReader).shouldHaveNoInteractions();
        then(unreadCountUpdateBroadcaster).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("구독 해제는 채팅방 구독 저장소에 위임한다")
    void unsubscribeToChatRoom_removesSubscriber() {
        // given
        Long chatRoomId = 1L;
        Long memberId = 10L;

        // when
        chatRoomSubscriptionService.unsubscribeToChatRoom(chatRoomId, memberId);

        // then
        then(chatRoomSubscriptionStore).should().removeSubscriber(chatRoomId, memberId);
    }
}
