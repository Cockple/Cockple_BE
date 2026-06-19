package umc.cockple.demo.domain.chat.presentation.websocket.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatMessageSender;
import umc.cockple.demo.domain.chat.service.websocket.session.EncodedChatMessage;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessageSender implements ChatMessageSender {

    private final WebSocketSessionRegistry sessionRegistry;

    @Override
    public boolean send(Long memberId, EncodedChatMessage message) {
        WebSocketSession session = sessionRegistry.findOpenSession(memberId).orElse(null);
        if (session == null) {
            log.debug("WebSocket 전송 대상 세션 없음 - 멤버: {}", memberId);
            return false;
        }

        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(message.payload()));
            }
            return true;
        } catch (Exception e) {
            log.error("WebSocket 메시지 전송 실패 - 멤버: {}", memberId, e);
            sessionRegistry.remove(memberId, session);
            return false;
        }
    }
}
