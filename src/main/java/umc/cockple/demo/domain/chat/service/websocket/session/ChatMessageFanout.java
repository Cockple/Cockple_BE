package umc.cockple.demo.domain.chat.service.websocket.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;
import umc.cockple.demo.domain.chat.realtime.ChatRealtimeProtocol;
import umc.cockple.demo.global.realtime.message.EncodedRealtimeMessage;
import umc.cockple.demo.global.realtime.publish.RealtimeMessagePublisher;
import umc.cockple.demo.global.realtime.publish.RealtimePublishResult;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatMessageFanout {

    private final ChatMessageSender legacyMessageSender;
    private final RealtimeMessagePublisher realtimeMessagePublisher;

    public boolean send(
            Long memberId,
            EncodedRealtimeMessage legacyMessage,
            WebSocketMessageType type,
            Object data
    ) {
        boolean legacyDelivered = sendLegacy(memberId, legacyMessage);
        boolean realtimeDelivered = sendRealtime(memberId, type, data);
        return legacyDelivered || realtimeDelivered;
    }

    private boolean sendLegacy(Long memberId, EncodedRealtimeMessage message) {
        if (message == null) {
            return false;
        }

        try {
            return legacyMessageSender.send(memberId, message);
        } catch (Exception e) {
            log.error("기존 채팅 WebSocket 메시지 전송 실패 - memberId: {}", memberId, e);
            return false;
        }
    }

    private boolean sendRealtime(Long memberId, WebSocketMessageType type, Object data) {
        try {
            RealtimePublishResult result = realtimeMessagePublisher.publish(
                    memberId,
                    ChatRealtimeProtocol.DOMAIN,
                    type.name(),
                    data
            );
            return result.deliveredToAnySession();
        } catch (Exception e) {
            log.error(
                    "공용 채팅 realtime 메시지 전송 실패 - memberId: {}, type: {}",
                    memberId, type, e
            );
            return false;
        }
    }
}
