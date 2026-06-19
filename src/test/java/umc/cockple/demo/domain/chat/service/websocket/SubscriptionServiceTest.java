package umc.cockple.demo.domain.chat.service.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.MessageType;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;
import umc.cockple.demo.domain.chat.repository.redis.ChatRoomSubscriptionStore;
import umc.cockple.demo.domain.chat.service.websocket.broadcast.ChatRoomListUpdateBroadcaster;
import umc.cockple.demo.domain.chat.service.websocket.broadcast.ChatRoomListUpdateData;
import umc.cockple.demo.domain.chat.service.websocket.broadcast.ChatRoomMessageBroadcaster;
import umc.cockple.demo.domain.chat.service.websocket.broadcast.UnreadCountUpdateBroadcaster;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatMessageSender;
import umc.cockple.demo.domain.chat.service.websocket.subscription.support.ActiveChatRoomSubscriberReader;
import umc.cockple.demo.domain.chat.service.websocket.subscription.support.SubscribeReadStatusService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionService")
class SubscriptionServiceTest {

    @Mock private SubscribeReadStatusService subscribeReadStatusService;
    @Mock private ChatRoomSubscriptionStore chatRoomSubscriptionStore;
    @Mock private ChatRoomMessageBroadcaster chatRoomMessageBroadcaster;
    @Mock private UnreadCountUpdateBroadcaster unreadCountUpdateBroadcaster;
    @Mock private ChatRoomListUpdateBroadcaster chatRoomListUpdateBroadcaster;
    @Mock private ChatMessageSender messageSender;
    @Mock private ActiveChatRoomSubscriberReader activeChatRoomSubscriberReader;

    private SubscriptionService subscriptionService;

    @BeforeEach
    void setUp() {
        subscriptionService = new SubscriptionService(
                subscribeReadStatusService,
                chatRoomSubscriptionStore,
                chatRoomMessageBroadcaster,
                unreadCountUpdateBroadcaster,
                chatRoomListUpdateBroadcaster,
                messageSender,
                activeChatRoomSubscriberReader
        );
    }

    @Test
    @DisplayName("활성 구독자 조회는 Redis 구독자 중 열린 세션 멤버만 반환한다")
    void getActiveSubscribers_returnsOpenSubscribers() {
        // given
        Long chatRoomId = 1L;
        given(activeChatRoomSubscriberReader.findActiveSubscribers(chatRoomId)).willReturn(List.of(10L));

        // when
        List<Long> activeSubscribers = subscriptionService.getActiveSubscribers(chatRoomId);

        // then
        assertThat(activeSubscribers).containsExactly(10L);
    }

    @Test
    @DisplayName("안읽음 상태 업데이트 메시지는 WebSocket sender에 위임한다")
    void sendUnreadStatusUpdateToMember_delegatesToMessageSender() {
        // given
        Long memberId = 10L;

        WebSocketMessageDTO.UnreadStatusUpdateMessage message =
                WebSocketMessageDTO.UnreadStatusUpdateMessage.builder()
                        .type(WebSocketMessageType.UNREAD_STATUS_UPDATE)
                        .hasUnread(true)
                        .hasPartyUnread(true)
                        .hasDirectUnread(false)
                        .timestamp(LocalDateTime.of(2026, 5, 21, 13, 15))
                        .build();

        // when
        subscriptionService.sendUnreadStatusUpdateToMember(memberId, message);

        // then
        then(messageSender).should().send(memberId, message);
    }

    @Test
    @DisplayName("채팅 메시지 브로드캐스트는 활성 구독자 조회 후 broadcaster에 위임한다")
    void broadcastMessage_delegatesToChatRoomMessageBroadcaster() {
        // given
        Long chatRoomId = 1L;
        Long senderId = 10L;
        List<Long> activeSubscribers = List.of(senderId, 20L);
        WebSocketMessageDTO.MessageResponse message = createMessage(chatRoomId, senderId);

        given(activeChatRoomSubscriberReader.findActiveSubscribers(chatRoomId)).willReturn(activeSubscribers);

        // when
        subscriptionService.broadcastMessage(chatRoomId, message, senderId);

        // then
        then(chatRoomMessageBroadcaster).should()
                .broadcast(chatRoomId, message, activeSubscribers, senderId);
    }

    @Test
    @DisplayName("구독 시 읽음 처리 결과가 있으면 안읽은 수 업데이트 broadcaster에 위임한다")
    void subscribeToChatRoom_delegatesUnreadCountUpdatesToBroadcaster() {
        // given
        Long chatRoomId = 1L;
        Long memberId = 10L;
        List<Long> activeSubscribers = List.of(memberId, 20L);
        List<SubscribeReadStatusService.MessageUnreadUpdate> updates =
                List.of(new SubscribeReadStatusService.MessageUnreadUpdate(100L, 1));

        given(subscribeReadStatusService.markUnreadMessagesAsReadOnSubscribe(chatRoomId, memberId))
                .willReturn(updates);
        given(activeChatRoomSubscriberReader.findActiveSubscribers(chatRoomId)).willReturn(activeSubscribers);

        // when
        subscriptionService.subscribeToChatRoom(chatRoomId, memberId);

        // then
        then(chatRoomSubscriptionStore).should().addSubscriber(chatRoomId, memberId);
        then(unreadCountUpdateBroadcaster).should()
                .broadcast(chatRoomId, updates, activeSubscribers, memberId);
    }

    @Test
    @DisplayName("채팅방 목록 업데이트는 broadcaster에 위임한다")
    void broadcastChatRoomListUpdateToMembers_delegatesToBroadcaster() {
        // given
        Long chatRoomId = 1L;
        Map<Long, ChatRoomListUpdateData> memberUpdateData = Map.of(
                10L,
                ChatRoomListUpdateData.builder()
                        .lastMessage(WebSocketMessageDTO.ChatRoomListUpdate.LastMessageUpdate.builder()
                                .content("hello")
                                .timestamp(LocalDateTime.of(2026, 5, 21, 13, 15))
                                .messageType("TEXT")
                                .build())
                        .unreadCount(1)
                        .build()
        );

        // when
        subscriptionService.broadcastChatRoomListUpdateToMembers(chatRoomId, memberUpdateData);

        // then
        then(chatRoomListUpdateBroadcaster).should().broadcast(chatRoomId, memberUpdateData);
    }

    private WebSocketMessageDTO.MessageResponse createMessage(Long chatRoomId, Long senderId) {
        return WebSocketMessageDTO.MessageResponse.builder()
                .type(WebSocketMessageType.SEND)
                .chatRoomId(chatRoomId)
                .messageId(100L)
                .content("hello")
                .messageType(MessageType.TEXT)
                .images(List.of())
                .senderId(senderId)
                .senderName("sender")
                .timestamp(LocalDateTime.of(2026, 5, 21, 13, 15))
                .unreadCount(1)
                .build();
    }
}
