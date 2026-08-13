package umc.cockple.demo.domain.chat.service.websocket.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;
import umc.cockple.demo.domain.chat.realtime.ChatRealtimeProtocol;
import umc.cockple.demo.global.realtime.message.EncodedRealtimeMessage;
import umc.cockple.demo.global.realtime.publish.RealtimeMessagePublisher;
import umc.cockple.demo.global.realtime.publish.RealtimePublishResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@DisplayName("ChatMessageFanout")
class ChatMessageFanoutTest {

    private ChatMessageSender legacyMessageSender;
    private RealtimeMessagePublisher realtimeMessagePublisher;
    private ChatMessageFanout fanout;

    @BeforeEach
    void setUp() {
        legacyMessageSender = mock(ChatMessageSender.class);
        realtimeMessagePublisher = mock(RealtimeMessagePublisher.class);
        fanout = new ChatMessageFanout(legacyMessageSender, realtimeMessagePublisher);
    }

    @Test
    @DisplayName("기존 채팅 메시지와 공용 realtime envelope를 모두 전송한다")
    void sendsToLegacyAndRealtimeEndpoints() {
        Long memberId = 10L;
        EncodedRealtimeMessage legacyMessage = new EncodedRealtimeMessage("legacy");
        Object data = new Object();
        given(legacyMessageSender.send(memberId, legacyMessage)).willReturn(true);
        given(realtimeMessagePublisher.publish(
                memberId,
                ChatRealtimeProtocol.DOMAIN,
                WebSocketMessageType.SEND.name(),
                data
        )).willReturn(new RealtimePublishResult(2, 2));

        boolean delivered = fanout.send(memberId, legacyMessage, WebSocketMessageType.SEND, data);

        assertThat(delivered).isTrue();
        then(legacyMessageSender).should().send(memberId, legacyMessage);
        then(realtimeMessagePublisher).should().publish(
                memberId,
                "CHAT",
                "SEND",
                data
        );
    }

    @Test
    @DisplayName("기존 직렬화 결과가 없어도 공용 realtime 전송을 시도한다")
    void sendsRealtimeWithoutLegacyMessage() {
        Long memberId = 10L;
        Object data = new Object();
        given(realtimeMessagePublisher.publish(memberId, "CHAT", "UNREAD_COUNT_UPDATE", data))
                .willReturn(new RealtimePublishResult(1, 1));

        boolean delivered = fanout.send(
                memberId,
                null,
                WebSocketMessageType.UNREAD_COUNT_UPDATE,
                data
        );

        assertThat(delivered).isTrue();
        then(legacyMessageSender).shouldHaveNoInteractions();
        then(realtimeMessagePublisher).should()
                .publish(memberId, "CHAT", "UNREAD_COUNT_UPDATE", data);
    }

    @Test
    @DisplayName("한 전송 방식만 성공해도 회원 전달 성공으로 판단한다")
    void succeedsWhenEitherEndpointDelivers() {
        Long memberId = 10L;
        EncodedRealtimeMessage legacyMessage = new EncodedRealtimeMessage("legacy");
        Object data = new Object();
        given(legacyMessageSender.send(memberId, legacyMessage)).willReturn(true);
        given(realtimeMessagePublisher.publish(memberId, "CHAT", "SEND", data))
                .willReturn(RealtimePublishResult.noTarget());

        boolean delivered = fanout.send(memberId, legacyMessage, WebSocketMessageType.SEND, data);

        assertThat(delivered).isTrue();
    }
}
