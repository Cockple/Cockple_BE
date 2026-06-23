package umc.cockple.demo.domain.chat.presentation.websocket.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.service.websocket.session.EncodedChatMessage;

@Component
@Slf4j
public class WebSocketSessionMessageSender {

    public boolean send(WebSocketSession session, EncodedChatMessage message) {
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(message.payload()));
            }
            return true;
        } catch (Exception e) {
            log.error("WebSocket 메시지 전송 실패 - 세션: {}", session.getId(), e);
            return false;
        }
    }
}
