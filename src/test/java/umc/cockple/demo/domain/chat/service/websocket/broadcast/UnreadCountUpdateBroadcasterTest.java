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
import umc.cockple.demo.domain.chat.service.websocket.session.ChatMessageFanout;
import umc.cockple.demo.global.realtime.message.RealtimeMessageEncoder;
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
    @Mock private ChatMessageFanout messageFanout;

    private UnreadCountUpdateBroadcaster broadcaster;

    @BeforeEach
    void setUp() {
        broadcaster = new UnreadCountUpdateBroadcaster(messageEncoder, messageFanout);
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
        given(messageFanout.send(
                org.mockito.ArgumentMatchers.eq(20L),
                org.mockito.ArgumentMatchers.eq(encodedMessage),
                org.mockito.ArgumentMatchers.eq(WebSocketMessageType.UNREAD_COUNT_UPDATE),
                any(WebSocketMessageDTO.UnreadCountUpdateMessage.class)
        )).willReturn(true);

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

        then(messageFanout).should().send(
                20L,
                encodedMessage,
                WebSocketMessageType.UNREAD_COUNT_UPDATE,
                message
        );
        then(messageFanout).shouldHaveNoMoreInteractions();
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
        then(messageFanout).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("기존 메시지 직렬화에 실패해도 공용 realtime 전송은 시도한다")
    void broadcast_attemptsRealtimeWhenLegacySerializationFails() {
        // given
        UnreadCountUpdate update =
                new UnreadCountUpdate(100L, 2);
        given(messageEncoder.encode(any(WebSocketMessageDTO.UnreadCountUpdateMessage.class)))
                .willReturn(Optional.empty());

        // when
        broadcaster.broadcast(1L, List.of(update), List.of(20L), 10L);

        // then
        ArgumentCaptor<WebSocketMessageDTO.UnreadCountUpdateMessage> messageCaptor =
                ArgumentCaptor.forClass(WebSocketMessageDTO.UnreadCountUpdateMessage.class);
        then(messageEncoder).should().encode(messageCaptor.capture());
        then(messageFanout).should().send(
                20L,
                null,
                WebSocketMessageType.UNREAD_COUNT_UPDATE,
                messageCaptor.getValue()
        );
    }
}
