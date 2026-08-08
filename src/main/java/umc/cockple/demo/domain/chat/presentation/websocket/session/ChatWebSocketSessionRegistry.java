package umc.cockple.demo.domain.chat.presentation.websocket.session;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatSessionRegistry;
import umc.cockple.demo.global.realtime.session.RealtimeEndpoint;
import umc.cockple.demo.global.realtime.session.RealtimeSessionRegistry;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ChatWebSocketSessionRegistry implements ChatSessionRegistry {

    static final RealtimeEndpoint LEGACY_CHAT_ENDPOINT = RealtimeEndpoint.of("legacy-chat");

    private final RealtimeSessionRegistry realtimeSessionRegistry;

    public void register(Long memberId, WebSocketSession session) {
        realtimeSessionRegistry.register(memberId, LEGACY_CHAT_ENDPOINT, session);
    }

    public void remove(Long memberId, WebSocketSession session) {
        realtimeSessionRegistry.remove(memberId, session);
    }

    public Optional<WebSocketSession> findOpenSession(Long memberId) {
        return realtimeSessionRegistry.findLatestOpenSession(memberId, LEGACY_CHAT_ENDPOINT);
    }

    @Override
    public List<Long> findOpenMemberIds(Collection<Long> memberIds) {
        return realtimeSessionRegistry.findOpenMemberIds(memberIds, LEGACY_CHAT_ENDPOINT);
    }
}
