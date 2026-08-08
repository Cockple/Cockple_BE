package umc.cockple.demo.domain.chat.service.websocket.session;

import umc.cockple.demo.global.realtime.message.EncodedRealtimeMessage;

public interface ChatMessageSender {

    boolean send(Long memberId, EncodedRealtimeMessage message);
}
