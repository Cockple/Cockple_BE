package umc.cockple.demo.domain.chat.presentation.websocket.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatMessageSender;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessageSender implements ChatMessageSender {

    private final ObjectMapper objectMapper;
    private final WebSocketSessionRegistry sessionRegistry;

    @Override
    public boolean send(Long memberId, Object message) {
        return serialize(message)
                .map(messageJson -> sendSerialized(memberId, messageJson))
                .orElse(false);
    }

    @Override
    public Optional<String> serialize(Object message) {
        try {
            return Optional.of(objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            log.error("WebSocket 메시지 JSON 변환 실패", e);
            return Optional.empty();
        }
    }

    @Override
    public boolean sendSerialized(Long memberId, String messageJson) {
        WebSocketSession session = sessionRegistry.findOpenSession(memberId).orElse(null);
        if (session == null) {
            log.debug("WebSocket 전송 대상 세션 없음 - 멤버: {}", memberId);
            sessionRegistry.remove(memberId);
            return false;
        }

        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(messageJson));
            }
            return true;
        } catch (Exception e) {
            log.error("WebSocket 메시지 전송 실패 - 멤버: {}", memberId, e);
            sessionRegistry.remove(memberId);
            return false;
        }
    }
}
