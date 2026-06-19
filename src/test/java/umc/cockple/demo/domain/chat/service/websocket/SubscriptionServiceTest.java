package umc.cockple.demo.domain.chat.service.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;
import umc.cockple.demo.domain.chat.repository.redis.ChatListSubscriptionStore;
import umc.cockple.demo.domain.chat.repository.redis.ChatRoomSubscriptionStore;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatMessageSender;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatSessionRegistry;
import umc.cockple.demo.domain.chat.service.websocket.subscription.support.SubscribeReadStatusService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionService")
class SubscriptionServiceTest {

    @Mock private SubscribeReadStatusService subscribeReadStatusService;
    @Mock private ChatRoomSubscriptionStore chatRoomSubscriptionStore;
    @Mock private ChatListSubscriptionStore chatListSubscriptionStore;
    @Mock private ChatMessageSender messageSender;
    @Mock private ChatSessionRegistry sessionRegistry;

    private SubscriptionService subscriptionService;

    @BeforeEach
    void setUp() {
        subscriptionService = new SubscriptionService(
                subscribeReadStatusService,
                chatRoomSubscriptionStore,
                chatListSubscriptionStore,
                messageSender,
                sessionRegistry
        );
    }

    @Test
    @DisplayName("활성 구독자 조회는 Redis 구독자 중 열린 세션 멤버만 반환한다")
    void getActiveSubscribers_returnsOpenSubscribers() {
        // given
        Long chatRoomId = 1L;
        Set<Long> redisSubscribers = Set.of(10L, 20L);
        given(chatRoomSubscriptionStore.getSubscribers(chatRoomId)).willReturn(redisSubscribers);
        given(sessionRegistry.findOpenMemberIds(redisSubscribers)).willReturn(List.of(10L));

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
}
