package umc.cockple.demo.domain.chat.service.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;
import umc.cockple.demo.domain.chat.repository.redis.ChatListSubscriptionStore;
import umc.cockple.demo.domain.chat.repository.redis.ChatRoomSubscriptionStore;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatWebSocketSessionRegistry;
import umc.cockple.demo.domain.chat.service.websocket.subscription.support.SubscribeReadStatusService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
    @Mock private ChatWebSocketSessionRegistry sessionRegistry;
    @Mock private WebSocketSession session;

    private SubscriptionService subscriptionService;

    @BeforeEach
    void setUp() {
        subscriptionService = new SubscriptionService(
                new ObjectMapper().findAndRegisterModules(),
                subscribeReadStatusService,
                chatRoomSubscriptionStore,
                chatListSubscriptionStore,
                sessionRegistry
        );
    }

    @Test
    @DisplayName("세션 등록은 세션 저장소에 위임한다")
    void addSession_delegatesToSessionRegistry() {
        // given
        Long memberId = 10L;

        // when
        subscriptionService.addSession(memberId, session);

        // then
        then(sessionRegistry).should().register(memberId, session);
    }

    @Test
    @DisplayName("세션 제거는 세션 저장소에 위임한다")
    void removeSession_delegatesToSessionRegistry() {
        // given
        Long memberId = 10L;

        // when
        subscriptionService.removeSession(memberId);

        // then
        then(sessionRegistry).should().remove(memberId);
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
    @DisplayName("안읽음 상태 업데이트 메시지를 대상 멤버 세션에 전송한다")
    void sendUnreadStatusUpdateToMember_sendsMessageToTargetSession() throws Exception {
        // given
        Long memberId = 10L;
        given(sessionRegistry.findOpenSession(memberId)).willReturn(Optional.of(session));

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
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        then(session).should().sendMessage(messageCaptor.capture());

        String payload = messageCaptor.getValue().getPayload();
        assertThat(payload).contains("\"type\":\"UNREAD_STATUS_UPDATE\"");
        assertThat(payload).contains("\"hasUnread\":true");
        assertThat(payload).contains("\"hasPartyUnread\":true");
        assertThat(payload).contains("\"hasDirectUnread\":false");
    }
}
