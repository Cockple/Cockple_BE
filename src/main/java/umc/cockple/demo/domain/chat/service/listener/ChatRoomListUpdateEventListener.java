package umc.cockple.demo.domain.chat.service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO.ChatRoomListUpdate.LastMessageUpdate;
import umc.cockple.demo.domain.chat.events.ChatRoomListUpdateEvent;
import umc.cockple.demo.domain.chat.service.websocket.ChatRoomListCacheService;
import umc.cockple.demo.domain.chat.service.websocket.broadcast.ChatRoomListUpdateBroadcaster;
import umc.cockple.demo.domain.chat.service.websocket.broadcast.ChatRoomListUpdateData;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatRoomListUpdateEventListener {

    private final ChatRoomListCacheService chatRoomListCacheService;
    private final ChatRoomListUpdateBroadcaster chatRoomListUpdateBroadcaster;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleChatRoomListUpdate(ChatRoomListUpdateEvent event) {
        log.info("채팅방 목록 업데이트 이벤트 처리 시작 - 채팅방: {}", event.chatRoomId());

        try {
            chatRoomListCacheService.evictLastMessage(event.chatRoomId());

            Map<Long, ChatRoomListUpdateData> memberUpdateData = new HashMap<>();

            LastMessageUpdate lastMessageUpdate = LastMessageUpdate.builder()
                    .content(event.content())
                    .timestamp(event.timestamp())
                    .messageType(event.messageType())
                    .build();

            for (Map.Entry<Long, Integer> entry : event.memberUnreadCounts().entrySet()) {
                Long memberId = entry.getKey();
                Integer unreadCount = entry.getValue();

                memberUpdateData.put(memberId, ChatRoomListUpdateData.builder()
                        .lastMessage(lastMessageUpdate)
                        .unreadCount(unreadCount)
                        .build());
            }

            chatRoomListUpdateBroadcaster.broadcast(event.chatRoomId(), memberUpdateData);

            log.info("채팅방 목록 업데이트 이벤트 처리 완료 - 채팅방: {}", event.chatRoomId());

        } catch (Exception e) {
            log.error("채팅방 목록 업데이트 이벤트 처리 중 오류 발생 - 채팅방: {}", event.chatRoomId(), e);
        }
    }
}
