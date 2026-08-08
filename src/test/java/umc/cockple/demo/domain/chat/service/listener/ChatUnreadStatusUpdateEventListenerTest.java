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
import umc.cockple.demo.domain.chat.service.query.ChatUnreadQueryService;
import umc.cockple.demo.global.realtime.message.RealtimeMessageEncoder;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatMessageSender;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatSessionRegistry;
import umc.cockple.demo.global.realtime.message.EncodedRealtimeMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatUnreadStatusUpdateEventListener")
class ChatUnreadStatusUpdateEventListenerTest {

    @Mock private ChatMessageSender chatMessageSender;
    @Mock private RealtimeMessageEncoder chatMessageEncoder;
    @Mock private ChatUnreadQueryService chatUnreadQueryService;
    @Mock private ChatSessionRegistry chatSessionRegistry;

    @InjectMocks
    private ChatUnreadStatusUpdateEventListener listener;

    @Test
    @DisplayName("안읽음 상태 업데이트 이벤트는 REST 조회와 같은 기준으로 멤버별 payload를 전송한다")
    void handleChatUnreadStatusUpdate_sendsMemberSpecificUnreadStatus() {
        // given
        Long partyUnreadMemberId = 101L;
        Long directUnreadMemberId = 102L;
        List<Long> targetMemberIds = List.of(partyUnreadMemberId, directUnreadMemberId);
        Map<Long, ChatUnreadQueryService.UnreadStatus> unreadStatuses = new LinkedHashMap<>();
        unreadStatuses.put(partyUnreadMemberId, new ChatUnreadQueryService.UnreadStatus(true, false));
        unreadStatuses.put(directUnreadMemberId, new ChatUnreadQueryService.UnreadStatus(false, true));

        given(chatSessionRegistry.findOpenMemberIds(targetMemberIds)).willReturn(targetMemberIds);
        given(chatUnreadQueryService.findUnreadStatusesByMembers(targetMemberIds)).willReturn(unreadStatuses);
        EncodedRealtimeMessage partyEncodedMessage = new EncodedRealtimeMessage("party-unread-json");
        EncodedRealtimeMessage directEncodedMessage = new EncodedRealtimeMessage("direct-unread-json");
        given(chatMessageEncoder.encode(any(WebSocketMessageDTO.UnreadStatusUpdateMessage.class)))
                .willReturn(Optional.of(partyEncodedMessage), Optional.of(directEncodedMessage));

        ChatUnreadStatusUpdateEvent event =
                ChatUnreadStatusUpdateEvent.of(targetMemberIds);

        // when
        listener.handleChatUnreadStatusUpdate(event);

        // then
        ArgumentCaptor<WebSocketMessageDTO.UnreadStatusUpdateMessage> messageCaptor =
                ArgumentCaptor.forClass(WebSocketMessageDTO.UnreadStatusUpdateMessage.class);

        then(chatMessageEncoder).should(times(2)).encode(messageCaptor.capture());
        then(chatMessageSender).should().send(partyUnreadMemberId, partyEncodedMessage);
        then(chatMessageSender).should().send(directUnreadMemberId, directEncodedMessage);

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

    @Test
    @DisplayName("열린 WebSocket 세션이 있는 멤버만 안읽음 상태를 조회하고 전송한다")
    void handleChatUnreadStatusUpdate_skipsMembersWithoutOpenSession() {
        // given
        Long openMemberId = 101L;
        Long offlineMemberId = 102L;
        List<Long> targetMemberIds = List.of(openMemberId, offlineMemberId);

        given(chatSessionRegistry.findOpenMemberIds(targetMemberIds)).willReturn(List.of(openMemberId));
        given(chatUnreadQueryService.findUnreadStatusesByMembers(List.of(openMemberId)))
                .willReturn(Map.of(openMemberId, new ChatUnreadQueryService.UnreadStatus(false, true)));
        EncodedRealtimeMessage encodedMessage = new EncodedRealtimeMessage("open-member-unread-json");
        given(chatMessageEncoder.encode(any(WebSocketMessageDTO.UnreadStatusUpdateMessage.class)))
                .willReturn(Optional.of(encodedMessage));

        ChatUnreadStatusUpdateEvent event = ChatUnreadStatusUpdateEvent.of(targetMemberIds);

        // when
        listener.handleChatUnreadStatusUpdate(event);

        // then
        then(chatUnreadQueryService).should().findUnreadStatusesByMembers(List.of(openMemberId));
        then(chatMessageSender).should(times(1)).send(eq(openMemberId), eq(encodedMessage));
    }

    @Test
    @DisplayName("열린 WebSocket 세션이 없으면 안읽음 상태를 조회하지 않는다")
    void handleChatUnreadStatusUpdate_returnsWhenNoOpenSession() {
        // given
        List<Long> targetMemberIds = List.of(101L, 102L);
        given(chatSessionRegistry.findOpenMemberIds(targetMemberIds)).willReturn(List.of());

        ChatUnreadStatusUpdateEvent event = ChatUnreadStatusUpdateEvent.of(targetMemberIds);

        // when
        listener.handleChatUnreadStatusUpdate(event);

        // then
        then(chatUnreadQueryService).shouldHaveNoInteractions();
        then(chatMessageEncoder).shouldHaveNoInteractions();
        then(chatMessageSender).shouldHaveNoInteractions();
    }
}
