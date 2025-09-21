package umc.cockple.demo.domain.chat.service.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO.ChatRoomListUpdate.LastMessageUpdate;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final ObjectMapper objectMapper;

    private final SubscriptionReadProcessingService subscriptionReadProcessingService;
    private final RedisSubscriptionService redisSubscriptionService;
    private final ChatListSubscriptionService chatListSubscriptionService;

    private final Map<Long, WebSocketSession> memberSessions = new ConcurrentHashMap<>();

    public void addSession(Long memberId, WebSocketSession session) {
        memberSessions.put(memberId, session);
    }

    public void removeSession(Long memberId) {
        memberSessions.remove(memberId);
        log.info("로컬 세션 제거 - 멤버: {}", memberId);
    }

    public void subscribeToChatRoom(Long chatRoomId, Long memberId) {
        redisSubscriptionService.addSubscriber(chatRoomId, memberId);
        log.info("채팅방 구독 - 채팅방: {}, 사용자: {}", chatRoomId, memberId);

        List<SubscriptionReadProcessingService.MessageUnreadUpdate> updates =
                subscriptionReadProcessingService.processUnreadMessagesOnSubscribe(chatRoomId, memberId);

        if (!updates.isEmpty()) {
            broadcastUnreadCountUpdates(chatRoomId, updates, memberId);
            log.info("구독으로 인한 안읽은 수 업데이트 브로드캐스트 완료 - 업데이트된 메시지 수: {}", updates.size());
        }
    }

    public void unsubscribeToChatRoom(Long chatRoomId, Long memberId) {
        redisSubscriptionService.removeSubscriber(chatRoomId, memberId);
        log.info("채팅방 구독 해제 완료 - 채팅방: {}, 사용자: {}", chatRoomId, memberId);
    }

    public void broadcastMessage(Long chatRoomId, WebSocketMessageDTO.MessageResponse message, Long senderId) {
        broadcastToChatRoom(chatRoomId, message, senderId);
    }

    public void broadcastSystemMessage(Long chatRoomId, WebSocketMessageDTO.MessageResponse message) {
        broadcastToChatRoom(chatRoomId, message, null);
    }

    public List<Long> getActiveSubscribers(Long chatRoomId) {
        Set<Long> redisSubscribers = redisSubscriptionService.getSubscribers(chatRoomId);

        return redisSubscribers.stream()
                .filter(memberId -> {
                    WebSocketSession session = memberSessions.get(memberId);
                    return session != null && session.isOpen();
                })
                .toList();
    }

    private void broadcastToChatRoom(Long chatRoomId, WebSocketMessageDTO.MessageResponse message, Long excludedMemberId) {
        List<Long> subscribers = getActiveSubscribers(chatRoomId);
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

        List<Long> successMembers = new ArrayList<>();
        List<Long> failedMembers = new ArrayList<>();

        // 메시지 브로드캐스트
        for (Long memberId : subscribers) {
            if (memberId.equals(excludedMemberId)) {
                continue;
            }

            WebSocketSession session = memberSessions.get(memberId);
            if (session != null && session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(messageJson));
                    }
                    successMembers.add(memberId);
                } catch (Exception e) {
                    log.error("메시지 전송 실패 - 사용자: {}", memberId, e);
                    failedMembers.add(memberId);
                }
            } else {
                failedMembers.add(memberId);
            }
        }

        failedMembers.forEach(memberSessions::remove);

        log.info("브로드캐스트 완료 - 채팅방: {}, 성공: {}명, 실패: {}명", chatRoomId, successMembers.size(), failedMembers.size());
    }

    private void broadcastUnreadCountUpdates(
            Long chatRoomId, List<SubscriptionReadProcessingService.MessageUnreadUpdate> updates, Long excludedMemberId) {
        List<Long> subscribers = getActiveSubscribers(chatRoomId);
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }

        for (SubscriptionReadProcessingService.MessageUnreadUpdate update : updates) {
            try {
                WebSocketMessageDTO.UnreadCountUpdateMessage updateMessage = WebSocketMessageDTO.UnreadCountUpdateMessage.builder()
                        .type(WebSocketMessageType.UNREAD_COUNT_UPDATE)
                        .chatRoomId(chatRoomId)
                        .messageId(update.messageId())
                        .newUnreadCount(update.newUnreadCount())
                        .timestamp(LocalDateTime.now())
                        .build();

                String messageJson = objectMapper.writeValueAsString(updateMessage);

                int successCount = 0;
                for (Long memberId : subscribers) {
                    if (memberId.equals(excludedMemberId)) {
                        continue;
                    }

                    WebSocketSession session = memberSessions.get(memberId);
                    if (session != null && session.isOpen()) {
                        try {
                            synchronized (session) {
                                session.sendMessage(new TextMessage(messageJson));
                            }
                            successCount++;
                        } catch (Exception e) {
                            log.error("안읽은 수 업데이트 브로드캐스트 실패 - 사용자: {}, 메시지: {}",
                                    memberId, update.messageId(), e);
                        }
                    }
                }

                log.debug("메시지 {} 안읽은 수 업데이트 브로드캐스트 완료 - 성공: {}명, 새 안읽은 수: {}",
                        update.messageId(), successCount, update.newUnreadCount());

            } catch (Exception e) {
                log.error("안읽은 수 업데이트 메시지 생성 실패 - 메시지: {}", update.messageId(), e);
            }
        }
    }

    public void broadcastChatRoomListUpdateToMembers(
            Long chatRoomId,
            Map<Long, ChatRoomListUpdateData> memberUpdateData) {
        log.info("채팅방 목록 업데이트 개별 브로드캐스트 시작 - 채팅방: {}, 대상자: {}명", chatRoomId, memberUpdateData.size());

        Set<Long> chatListSubscribers = chatListSubscriptionService.getChatListSubscribers(chatRoomId);

        int successCount = 0;
        int failedCount = 0;

        for (Map.Entry<Long, ChatRoomListUpdateData> entry : memberUpdateData.entrySet()) {
            Long memberId = entry.getKey();

            if (!chatListSubscribers.contains(memberId)) {
                continue;
            }

            ChatRoomListUpdateData updateData = entry.getValue();
            WebSocketSession session = memberSessions.get(memberId);
            if (session != null && session.isOpen()) {
                try {
                    WebSocketMessageDTO.ChatRoomListUpdate message = WebSocketMessageDTO.ChatRoomListUpdate.builder()
                            .type(WebSocketMessageType.CHAT_ROOM_LIST_UPDATE)
                            .chatRoomId(chatRoomId)
                            .lastMessage(updateData.lastMessage())
                            .newUnreadCount(updateData.unreadCount())
                            .timestamp(LocalDateTime.now())
                            .build();

                    String messageJson = objectMapper.writeValueAsString(message);

                    synchronized (session) {
                        session.sendMessage(new TextMessage(messageJson));
                    }
                    successCount++;

                } catch (Exception e) {
                    log.error("채팅방 목록 업데이트 전송 실패 - 사용자: {}", memberId, e);
                    failedCount++;
                    memberSessions.remove(memberId);
                }
            } else {
                failedCount++;
            }
        }

        log.info("채팅방 목록 업데이트 개별 브로드캐스트 완료 - 성공: {}명, 실패: {}명", successCount, failedCount);
    }

    @Builder
    public record ChatRoomListUpdateData(
            LastMessageUpdate lastMessage,
            int unreadCount
    ) {
    }
}
