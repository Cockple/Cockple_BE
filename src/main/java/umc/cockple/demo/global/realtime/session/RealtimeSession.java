package umc.cockple.demo.global.realtime.session;

import org.springframework.web.socket.WebSocketSession;

import java.util.Objects;

public record RealtimeSession(
        Long memberId,
        RealtimeEndpoint endpoint,
        String sessionId,
        WebSocketSession webSocketSession,
        long registrationOrder
) {

    public RealtimeSession {
        Objects.requireNonNull(memberId, "memberId는 null일 수 없습니다.");
        Objects.requireNonNull(endpoint, "endpoint는 null일 수 없습니다.");
        Objects.requireNonNull(webSocketSession, "webSocketSession은 null일 수 없습니다.");
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("WebSocket session ID는 비어 있을 수 없습니다.");
        }
    }

    public boolean isOpen() {
        return webSocketSession.isOpen();
    }
}
