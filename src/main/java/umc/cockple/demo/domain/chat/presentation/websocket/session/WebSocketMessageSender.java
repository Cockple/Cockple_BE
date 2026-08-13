package umc.cockple.demo.domain.chat.presentation.websocket.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatMessageSender;
import umc.cockple.demo.global.realtime.message.EncodedRealtimeMessage;
import umc.cockple.demo.global.realtime.session.WebSocketSessionMessageSender;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessageSender implements ChatMessageSender {

    private final ChatWebSocketSessionRegistry sessionRegistry;
    private final WebSocketSessionMessageSender sessionMessageSender;

    @Override
    public boolean send(Long memberId, EncodedRealtimeMessage message) {
        WebSocketSession session = sessionRegistry.findOpenSession(memberId).orElse(null);
        if (session == null) {
            log.debug("WebSocket 전송 대상 세션 없음 - 멤버: {}", memberId);
            return false;
        }

        if (sessionMessageSender.send(session, message)) {
            return true;
        }

        log.error("WebSocket 메시지 전송 실패 - 멤버: {}", memberId);
        sessionRegistry.remove(memberId, session);
        return false;
    }
}
