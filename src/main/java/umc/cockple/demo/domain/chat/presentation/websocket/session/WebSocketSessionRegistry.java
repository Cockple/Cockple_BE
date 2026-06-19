package umc.cockple.demo.domain.chat.presentation.websocket.session;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatSessionRegistry;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionRegistry implements ChatSessionRegistry {

    private final Map<Long, WebSocketSession> memberSessions = new ConcurrentHashMap<>();

    public void register(Long memberId, WebSocketSession session) {
        memberSessions.put(memberId, session);
    }

    public void remove(Long memberId) {
        memberSessions.remove(memberId);
    }

    public Optional<WebSocketSession> findOpenSession(Long memberId) {
        WebSocketSession session = memberSessions.get(memberId);
        if (session == null || !session.isOpen()) {
            return Optional.empty();
        }

        return Optional.of(session);
    }

    @Override
    public List<Long> findOpenMemberIds(Collection<Long> memberIds) {
        return memberIds.stream()
                .filter(memberId -> findOpenSession(memberId).isPresent())
                .toList();
    }
}
