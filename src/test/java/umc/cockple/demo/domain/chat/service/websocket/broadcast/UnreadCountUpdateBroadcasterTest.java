package umc.cockple.demo.domain.chat.service.websocket.broadcast;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;
import umc.cockple.demo.domain.chat.service.websocket.UnreadCountUpdate;
import umc.cockple.demo.global.realtime.message.RealtimeMessageEncoder;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatMessageSender;
import umc.cockple.demo.global.realtime.message.EncodedRealtimeMessage;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("UnreadCountUpdateBroadcaster")
class UnreadCountUpdateBroadcasterTest {

    @Mock private RealtimeMessageEncoder messageEncoder;
    @Mock private ChatMessageSender messageSender;

    private UnreadCountUpdateBroadcaster broadcaster;

    @BeforeEach
    void setUp() {
        broadcaster = new UnreadCountUpdateBroadcaster(messageEncoder, messageSender);
    }

    @Test
    @DisplayName("읽은 멤버를 제외하고 메시지별 안읽은 수 업데이트를 전송한다")
    void broadcast_sendsUnreadCountUpdateExceptExcludedMember() {
        // given
        Long chatRoomId = 1L;
        Long excludedMemberId = 10L;
        UnreadCountUpdate update =
                new UnreadCountUpdate(100L, 2);
        EncodedRealtimeMessage encodedMessage = new EncodedRealtimeMessage("unread-count-json");

        given(messageEncoder.encode(any(WebSocketMessageDTO.UnreadCountUpdateMessage.class)))
                .willReturn(Optional.of(encodedMessage));
        given(messageSender.send(20L, encodedMessage)).willReturn(true);

        // when
        broadcaster.broadcast(chatRoomId, List.of(update), List.of(excludedMemberId, 20L), excludedMemberId);

        // then
        ArgumentCaptor<WebSocketMessageDTO.UnreadCountUpdateMessage> messageCaptor =
                ArgumentCaptor.forClass(WebSocketMessageDTO.UnreadCountUpdateMessage.class);
        then(messageEncoder).should().encode(messageCaptor.capture());
        WebSocketMessageDTO.UnreadCountUpdateMessage message = messageCaptor.getValue();
        assertThat(message.type()).isEqualTo(WebSocketMessageType.UNREAD_COUNT_UPDATE);
        assertThat(message.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(message.messageId()).isEqualTo(update.messageId());
        assertThat(message.newUnreadCount()).isEqualTo(update.newUnreadCount());

        then(messageSender).should().send(20L, encodedMessage);
        then(messageSender).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("구독자가 없으면 메시지를 직렬화하지 않는다")
    void broadcast_doesNotSerializeWhenSubscribersEmpty() {
        // given
        UnreadCountUpdate update =
                new UnreadCountUpdate(100L, 2);

        // when
        broadcaster.broadcast(1L, List.of(update), List.of(), 10L);

        // then
        then(messageEncoder).shouldHaveNoInteractions();
        then(messageSender).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("직렬화 실패 시 전송하지 않고 다음 업데이트로 넘어간다")
    void broadcast_doesNotSendWhenSerializationFails() {
        // given
        UnreadCountUpdate update =
                new UnreadCountUpdate(100L, 2);
        given(messageEncoder.encode(any(WebSocketMessageDTO.UnreadCountUpdateMessage.class)))
                .willReturn(Optional.empty());

        // when
        broadcaster.broadcast(1L, List.of(update), List.of(20L), 10L);

        // then
        then(messageEncoder).should().encode(any(WebSocketMessageDTO.UnreadCountUpdateMessage.class));
        then(messageSender).shouldHaveNoMoreInteractions();
    }
}
