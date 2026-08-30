package umc.cockple.demo.domain.chat.presentation.realtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import umc.cockple.demo.global.realtime.routing.RealtimeResponder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@DisplayName("RealtimeChatCommandResponder")
class RealtimeChatCommandResponderTest {

    private RealtimeResponder realtimeResponder;
    private RealtimeChatCommandResponder responder;

    @BeforeEach
    void setUp() {
        realtimeResponder = mock(RealtimeResponder.class);
        responder = new RealtimeChatCommandResponder(realtimeResponder);
    }

    @Test
    @DisplayName("채팅 오류를 공용 realtime 오류로 전달한다")
    void delegatesError() {
        responder.sendError("CHAT_ERROR", "채팅 오류");

        then(realtimeResponder).should().sendError("CHAT_ERROR", "채팅 오류");
    }

    @Test
    @DisplayName("채팅방 구독 ACK를 공용 realtime 응답 data로 전달한다")
    void sendsRoomSubscriptionAcknowledgement() {
        ArgumentCaptor<Object> dataCaptor = ArgumentCaptor.forClass(Object.class);

        responder.acknowledgeRoomSubscription(20L, "SUBSCRIBE");

        then(realtimeResponder).should().send(eq("SUBSCRIBE"), dataCaptor.capture());
        RealtimeChatCommandResponder.RoomSubscriptionData data =
                (RealtimeChatCommandResponder.RoomSubscriptionData) dataCaptor.getValue();
        assertThat(data.chatRoomId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("채팅방 목록 구독 ACK를 공용 realtime 응답 data로 전달한다")
    void sendsChatListSubscriptionAcknowledgement() {
        List<Long> chatRoomIds = List.of(20L, 30L);
        ArgumentCaptor<Object> dataCaptor = ArgumentCaptor.forClass(Object.class);

        responder.acknowledgeChatListSubscription(chatRoomIds, "SUBSCRIBE_CHAT_LIST");

        then(realtimeResponder).should().send(eq("SUBSCRIBE_CHAT_LIST"), dataCaptor.capture());
        RealtimeChatCommandResponder.ChatListSubscriptionData data =
                (RealtimeChatCommandResponder.ChatListSubscriptionData) dataCaptor.getValue();
        assertThat(data.chatRoomIds()).containsExactlyElementsOf(chatRoomIds);
    }
}
