package umc.cockple.demo.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketBroadcastService {

    private final ObjectMapper objectMapper;

    private final Map<Long, Map<Long, WebSocketSession>> chatRoomSessions = new ConcurrentHashMap<>();
    private final Map<Long, WebSocketSession> memberSessions = new ConcurrentHashMap<>();

    public void addSessionToChatRoom(Long memberId, WebSocketSession session) {
        memberSessions.put(memberId, session);
    }

    public void removeSession(Long memberId) {
        memberSessions.remove(memberId);

        chatRoomSessions.forEach((chatRoomId, sessions) -> {
            sessions.remove(memberId);
            if (sessions.isEmpty()) {
                chatRoomSessions.remove(chatRoomId);
            }
        });
    }
}
