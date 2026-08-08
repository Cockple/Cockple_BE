package umc.cockple.demo.global.realtime.session;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RealtimeSessionRegistry {

    // 다중 세션 저장용
    private final Map<Long, ConcurrentMap<String, RealtimeSession>> memberSessions = new ConcurrentHashMap<>();
    private final AtomicLong registrationSequence = new AtomicLong();

    public void register(Long memberId, RealtimeEndpoint endpoint, WebSocketSession session) {
        RealtimeSession realtimeSession = new RealtimeSession(
                memberId,
                endpoint,
                session.getId(),
                session,
                registrationSequence.incrementAndGet()
        );

        memberSessions.compute(memberId, (ignored, sessions) -> {
            ConcurrentMap<String, RealtimeSession> currentSessions =
                    sessions == null ? new ConcurrentHashMap<>() : sessions;
            currentSessions.put(realtimeSession.sessionId(), realtimeSession);
            return currentSessions;
        });
    }

    public void remove(Long memberId, WebSocketSession session) {
        if (memberId == null || session == null || session.getId() == null) {
            return;
        }

        memberSessions.computeIfPresent(memberId, (ignored, sessions) -> {
            RealtimeSession registeredSession = sessions.get(session.getId());
            if (registeredSession != null && registeredSession.webSocketSession() == session) {
                sessions.remove(session.getId(), registeredSession);
            }
            return sessions.isEmpty() ? null : sessions;
        });
    }

    public Optional<WebSocketSession> findLatestOpenSession(Long memberId, RealtimeEndpoint endpoint) {
        return findOpenSessions(memberId, endpoint).stream()
                .findFirst()
                .map(RealtimeSession::webSocketSession);
    }

    public List<RealtimeSession> findOpenSessions(Long memberId, RealtimeEndpoint endpoint) {
        if (memberId == null || endpoint == null) {
            return List.of();
        }

        ConcurrentMap<String, RealtimeSession> sessions = memberSessions.get(memberId);
        if (sessions == null) {
            return List.of();
        }

        List<RealtimeSession> openSessions = sessions.values().stream()
                .filter(session -> session.endpoint().equals(endpoint))
                .filter(this::isOpenOrRemove)
                .sorted(Comparator.comparingLong(RealtimeSession::registrationOrder).reversed())
                .toList();

        return openSessions;
    }

    public List<Long> findOpenMemberIds(Collection<Long> memberIds, RealtimeEndpoint endpoint) {
        return memberIds.stream()
                .filter(memberId -> findLatestOpenSession(memberId, endpoint).isPresent())
                .toList();
    }

    private boolean isOpenOrRemove(RealtimeSession session) {
        if (session.isOpen()) {
            return true;
        }
        remove(session.memberId(), session.webSocketSession());
        return false;
    }
}
