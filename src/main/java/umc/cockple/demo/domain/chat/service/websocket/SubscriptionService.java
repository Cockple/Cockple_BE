package umc.cockple.demo.domain.chat.service.websocket;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO.ChatRoomListUpdate.LastMessageUpdate;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;
import umc.cockple.demo.domain.chat.repository.redis.ChatListSubscriptionStore;
import umc.cockple.demo.domain.chat.repository.redis.ChatRoomSubscriptionStore;
import umc.cockple.demo.domain.chat.service.websocket.broadcast.ChatRoomMessageBroadcaster;
import umc.cockple.demo.domain.chat.service.websocket.broadcast.UnreadCountUpdateBroadcaster;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatMessageSender;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatSessionRegistry;
import umc.cockple.demo.domain.chat.service.websocket.subscription.support.SubscribeReadStatusService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscribeReadStatusService subscribeReadStatusService;
    private final ChatRoomSubscriptionStore chatRoomSubscriptionStore;
    private final ChatListSubscriptionStore chatListSubscriptionStore;
    private final ChatRoomMessageBroadcaster chatRoomMessageBroadcaster;
    private final UnreadCountUpdateBroadcaster unreadCountUpdateBroadcaster;
    private final ChatMessageSender messageSender;
    private final ChatSessionRegistry sessionRegistry;

    public void subscribeToChatRoom(Long chatRoomId, Long memberId) {
        chatRoomSubscriptionStore.addSubscriber(chatRoomId, memberId);
        log.info("채팅방 구독 - 채팅방: {}, 사용자: {}", chatRoomId, memberId);

        List<SubscribeReadStatusService.MessageUnreadUpdate> updates =
                subscribeReadStatusService.markUnreadMessagesAsReadOnSubscribe(chatRoomId, memberId);

        if (!updates.isEmpty()) {
            List<Long> subscribers = getActiveSubscribers(chatRoomId);
            unreadCountUpdateBroadcaster.broadcast(chatRoomId, updates, subscribers, memberId);
            log.info("구독으로 인한 안읽은 수 업데이트 브로드캐스트 완료 - 업데이트된 메시지 수: {}", updates.size());
        }
    }

    public void unsubscribeToChatRoom(Long chatRoomId, Long memberId) {
        chatRoomSubscriptionStore.removeSubscriber(chatRoomId, memberId);
        log.info("채팅방 구독 해제 완료 - 채팅방: {}, 사용자: {}", chatRoomId, memberId);
    }

    public void broadcastMessage(Long chatRoomId, WebSocketMessageDTO.MessageResponse message, Long senderId) {
        List<Long> subscribers = getActiveSubscribers(chatRoomId);
        chatRoomMessageBroadcaster.broadcast(chatRoomId, message, subscribers, senderId);
    }

    public void broadcastSystemMessage(Long chatRoomId, WebSocketMessageDTO.MessageResponse message) {
        List<Long> subscribers = getActiveSubscribers(chatRoomId);
        chatRoomMessageBroadcaster.broadcast(chatRoomId, message, subscribers, null);
    }

    public void sendUnreadStatusUpdateToMember(
            Long memberId,
            WebSocketMessageDTO.UnreadStatusUpdateMessage message) {
        if (messageSender.send(memberId, message)) {
            log.debug("안읽음 상태 업데이트 전송 완료 - 멤버: {}", memberId);
        }
    }

    public List<Long> getActiveSubscribers(Long chatRoomId) {
        Set<Long> redisSubscribers = chatRoomSubscriptionStore.getSubscribers(chatRoomId);

        return sessionRegistry.findOpenMemberIds(redisSubscribers);
    }

    public void broadcastChatRoomListUpdateToMembers(
            Long chatRoomId,
            Map<Long, ChatRoomListUpdateData> memberUpdateData) {
        log.info("채팅방 목록 업데이트 개별 브로드캐스트 시작 - 채팅방: {}, 대상자: {}명", chatRoomId, memberUpdateData.size());

        Set<Long> chatListSubscribers = chatListSubscriptionStore.getChatListSubscribers(chatRoomId);

        int successCount = 0;
        int failedCount = 0;

        for (Map.Entry<Long, ChatRoomListUpdateData> entry : memberUpdateData.entrySet()) {
            Long memberId = entry.getKey();

            if (!chatListSubscribers.contains(memberId)) {
                continue;
            }

            ChatRoomListUpdateData updateData = entry.getValue();
            WebSocketMessageDTO.ChatRoomListUpdate message = WebSocketMessageDTO.ChatRoomListUpdate.builder()
                    .type(WebSocketMessageType.CHAT_ROOM_LIST_UPDATE)
                    .chatRoomId(chatRoomId)
                    .lastMessage(updateData.lastMessage())
                    .newUnreadCount(updateData.unreadCount())
                    .timestamp(LocalDateTime.now())
                    .build();

            if (messageSender.send(memberId, message)) {
                successCount++;
            } else {
                log.error("채팅방 목록 업데이트 전송 실패 - 사용자: {}", memberId);
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
