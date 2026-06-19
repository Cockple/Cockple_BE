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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionService")
class SubscriptionServiceTest {

    @Mock private SubscriptionReadProcessingService subscriptionReadProcessingService;
    @Mock private RedisSubscriptionService redisSubscriptionService;
    @Mock private ChatListSubscriptionService chatListSubscriptionService;
    @Mock private WebSocketSession session;

    private SubscriptionService subscriptionService;

    @BeforeEach
    void setUp() {
        subscriptionService = new SubscriptionService(
                new ObjectMapper().findAndRegisterModules(),
                subscriptionReadProcessingService,
                redisSubscriptionService,
                chatListSubscriptionService
        );
    }

    @Test
    @DisplayName("안읽음 상태 업데이트 메시지를 대상 멤버 세션에 전송한다")
    void sendUnreadStatusUpdateToMember_sendsMessageToTargetSession() throws Exception {
        // given
        Long memberId = 10L;
        given(session.isOpen()).willReturn(true);
        subscriptionService.addSession(memberId, session);

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
