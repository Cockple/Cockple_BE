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

    public void subscribeToChatRoom(Long chatRoomId, Long memberId) {
        chatRoomSubscriptions.computeIfAbsent(chatRoomId, k -> ConcurrentHashMap.newKeySet())
                .add(memberId);

        log.info("채팅방 구독 - 채팅방: {}, 사용자: {}", chatRoomId, memberId);
    }

    public void broadcastMessage(Long chatRoomId, WebSocketMessageDTO.MessageResponse message, Long senderId){
        broadcastToChatRoom(chatRoomId, message, senderId);
    }

    public void broadcastSystemMessage(Long chatRoomId, WebSocketMessageDTO.MessageResponse message) {
        broadcastToChatRoom(chatRoomId, message, null);
    }

    private void broadcastToChatRoom(Long chatRoomId, WebSocketMessageDTO.MessageResponse message, Long excludedMemberId) {
        Set<Long> subscribers = chatRoomSubscriptions.get(chatRoomId);
        if (subscribers == null || subscribers.isEmpty()) {
            log.info("채팅방 {}에 구독 중인 사용자가 없습니다.", chatRoomId);
            return;
        }

        String messageJson;
        try {
            messageJson = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("메시지를 JSON으로 변환하는데 실패했습니다", e);
            return;
        }

        List<Long> failedMembers = new ArrayList<>();
        int successCount = 0;

        for (Long memberId : subscribers) {
            if(memberId.equals(excludedMemberId)) {
                continue;
            }

            WebSocketSession session = memberSessions.get(memberId);

            if (session != null && session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(messageJson));
                    }
                    successCount++;
                    log.debug("메시지 전송 성공 - 사용자: {}", memberId);
                } catch (Exception e) {
                    log.error("메시지 전송 실패 - 사용자: {}", memberId, e);
                    failedMembers.add(memberId);
                }
            } else {
                log.warn("유효하지 않은 세션 - 사용자: {}", memberId);
                failedMembers.add(memberId);
            }
        }

        cleanupFailedSubscriptions(chatRoomId, failedMembers);

        log.info("브로드캐스트 완료 - 채팅방: {}, 성공: {}명, 실패: {}명", chatRoomId, successCount, failedMembers.size());
    }

    private void cleanupFailedSubscriptions(Long chatRoomId, List<Long> failedMemberIds) {
        if (failedMemberIds.isEmpty()) return;

        Set<Long> subscribers = chatRoomSubscriptions.get(chatRoomId);
        if (subscribers != null) {
            int removedCount = 0;
            for (Long failedMemberId : failedMemberIds) {
                if (subscribers.remove(failedMemberId)) {
                    removedCount++;
                    log.info("구독에서 제거된 사용자: {} (채팅방: {})", failedMemberId, chatRoomId);
                }
            }

            if (subscribers.isEmpty()) {
                chatRoomSubscriptions.remove(chatRoomId);
                log.info("빈 채팅방 구독자 목록 제거 - 채팅방: {}", chatRoomId);
            }

            log.info("구독 세션 정리 완료 - 채팅방: {}, 제거된 구독: {}명, 남은 구독: {}명",
                    chatRoomId, removedCount, subscribers.size());
        }
    }
}
