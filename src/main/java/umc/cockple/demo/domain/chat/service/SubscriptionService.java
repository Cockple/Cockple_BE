package umc.cockple.demo.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final ObjectMapper objectMapper;

    // 세션 관리
    private final Map<Long, WebSocketSession> memberSessions = new ConcurrentHashMap<>();
    // 구독 관리
    private final Map<Long, Set<Long>> chatRoomSubscriptions = new ConcurrentHashMap<>();

    public void addSession(Long memberId, WebSocketSession session) {
        memberSessions.put(memberId, session);
    }

    public void removeSession(Long memberId) {
        memberSessions.remove(memberId);

        chatRoomSubscriptions.forEach((chatRoomId, subscribers) -> {
            subscribers.remove(memberId);
            if (subscribers.isEmpty()) {
                chatRoomSubscriptions.remove(chatRoomId);
            }
        });
    }

    public void broadcastToChatRoom(Long chatRoomId, WebSocketMessageDTO.Response message) {
        Map<Long, WebSocketSession> sessions = chatRoomSessions.get(chatRoomId);
        if (sessions == null || sessions.isEmpty()) {
            log.info("채팅방 {}에 구독 중인 세션이 없습니다.", chatRoomId);
            return;
        }

        String messageJson;
        try {
            messageJson = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("메시지를 JSON으로 변환하는데 실패했습니다", e);
            return;
        }

        Map<Long, WebSocketSession> sessionsCopy = new HashMap<>(sessions);
        List<Long> failedSessions = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successCount = new AtomicInteger(0);

        sessionsCopy.entrySet().parallelStream().forEach(entry -> {
            Long memberId = entry.getKey();
            WebSocketSession session = entry.getValue();

            if (session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(messageJson));
                    }
                    successCount.incrementAndGet();
                    log.debug("메시지 전송 성공 - 사용자: {}, 세션: {}", memberId, session.getId());
                } catch (Exception e) {
                    log.error("메시지 전송 실패 - 사용자: {}, 세션: {}", memberId, session.getId(), e);
                    failedSessions.add(memberId);
                }
            } else {
                log.warn("닫힌 세션 발견 - 사용자: {}, 세션: {}", memberId, session.getId());
                failedSessions.add(memberId);
            }
        });

        cleanupFailedSessions(chatRoomId, failedSessions);

        log.info("브로드캐스트 완료 - 채팅방: {}, 성공: {}명, 실패: {}명", chatRoomId, successCount, failedSessions.size());
    }

    private void cleanupFailedSessions(Long chatRoomId, List<Long> failedSessionIds) {
        if (failedSessionIds.isEmpty()) return;

        Map<Long, WebSocketSession> sessions = chatRoomSessions.get(chatRoomId);
        if (sessions != null) {
            failedSessionIds.forEach(sessions::remove);

            if (sessions.isEmpty()) {
                chatRoomSessions.remove(chatRoomId);
                log.info("빈 채팅방 세션 맵 제거 - 채팅방: {}", chatRoomId);
            }
        }
    }
}
