package umc.cockple.demo.domain.chat.service.websocket.broadcast;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;
import umc.cockple.demo.domain.chat.repository.redis.ChatListSubscriptionStore;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatMessageEncoder;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatMessageSender;
import umc.cockple.demo.domain.chat.service.websocket.session.EncodedChatMessage;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatRoomListUpdateBroadcaster {

    private final ChatListSubscriptionStore chatListSubscriptionStore;
    private final ChatMessageEncoder messageEncoder;
    private final ChatMessageSender messageSender;

    public void broadcast(Long chatRoomId, Map<Long, ChatRoomListUpdateData> memberUpdateData) {
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

            EncodedChatMessage encodedMessage = messageEncoder.encode(message).orElse(null);
            if (encodedMessage != null && messageSender.send(memberId, encodedMessage)) {
                successCount++;
            } else {
                log.error("채팅방 목록 업데이트 전송 실패 - 사용자: {}", memberId);
                failedCount++;
            }
        }

        log.info("채팅방 목록 업데이트 개별 브로드캐스트 완료 - 성공: {}명, 실패: {}명", successCount, failedCount);
    }
}
