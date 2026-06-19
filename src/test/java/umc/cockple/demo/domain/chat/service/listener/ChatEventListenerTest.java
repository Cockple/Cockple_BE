package umc.cockple.demo.domain.chat.service.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;
import umc.cockple.demo.domain.chat.events.ChatUnreadStatusUpdateEvent;
import umc.cockple.demo.domain.chat.service.ChatUnreadQueryService;
import umc.cockple.demo.domain.chat.repository.redis.ChatListSubscriptionStore;
import umc.cockple.demo.domain.chat.service.websocket.ChatRoomListCacheService;
import umc.cockple.demo.domain.chat.service.websocket.send.ChatSendService;
import umc.cockple.demo.domain.chat.service.websocket.SubscriptionService;
import umc.cockple.demo.domain.notification.service.ChatPushNotificationService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatEventListener")
class ChatEventListenerTest {

    @Mock private ChatSendService chatSendService;
    @Mock private SubscriptionService subscriptionService;
    @Mock private ChatRoomListCacheService chatRoomListCacheService;
    @Mock private ChatListSubscriptionStore chatListSubscriptionStore;
    @Mock private ChatPushNotificationService chatPushNotificationService;
    @Mock private ChatUnreadQueryService chatUnreadQueryService;

    @InjectMocks
    private ChatEventListener chatEventListener;

    @Test
    @DisplayName("안읽음 상태 업데이트 이벤트는 REST 조회와 같은 기준으로 멤버별 payload를 전송한다")
    void handleChatUnreadStatusUpdate_sendsMemberSpecificUnreadStatus() {
        // given
        Long partyUnreadMemberId = 101L;
        Long directUnreadMemberId = 102L;

        given(chatUnreadQueryService.hasPartyUnreadMessages(partyUnreadMemberId)).willReturn(true);
        given(chatUnreadQueryService.hasDirectUnreadMessages(partyUnreadMemberId)).willReturn(false);
        given(chatUnreadQueryService.hasPartyUnreadMessages(directUnreadMemberId)).willReturn(false);
        given(chatUnreadQueryService.hasDirectUnreadMessages(directUnreadMemberId)).willReturn(true);

        ChatUnreadStatusUpdateEvent event =
                ChatUnreadStatusUpdateEvent.of(List.of(partyUnreadMemberId, directUnreadMemberId));

        // when
        chatEventListener.handleChatUnreadStatusUpdate(event);

        // then
        ArgumentCaptor<Long> memberIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<WebSocketMessageDTO.UnreadStatusUpdateMessage> messageCaptor =
                ArgumentCaptor.forClass(WebSocketMessageDTO.UnreadStatusUpdateMessage.class);

        then(subscriptionService).should(times(2))
                .sendUnreadStatusUpdateToMember(memberIdCaptor.capture(), messageCaptor.capture());

        assertThat(memberIdCaptor.getAllValues()).containsExactly(partyUnreadMemberId, directUnreadMemberId);

        WebSocketMessageDTO.UnreadStatusUpdateMessage partyUnreadMessage = messageCaptor.getAllValues().get(0);
        assertThat(partyUnreadMessage.type()).isEqualTo(WebSocketMessageType.UNREAD_STATUS_UPDATE);
        assertThat(partyUnreadMessage.hasUnread()).isTrue();
        assertThat(partyUnreadMessage.hasPartyUnread()).isTrue();
        assertThat(partyUnreadMessage.hasDirectUnread()).isFalse();

        WebSocketMessageDTO.UnreadStatusUpdateMessage directUnreadMessage = messageCaptor.getAllValues().get(1);
        assertThat(directUnreadMessage.type()).isEqualTo(WebSocketMessageType.UNREAD_STATUS_UPDATE);
        assertThat(directUnreadMessage.hasUnread()).isTrue();
        assertThat(directUnreadMessage.hasPartyUnread()).isFalse();
        assertThat(directUnreadMessage.hasDirectUnread()).isTrue();
    }
}
