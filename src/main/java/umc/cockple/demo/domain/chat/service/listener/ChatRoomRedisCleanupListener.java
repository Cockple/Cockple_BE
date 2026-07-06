package umc.cockple.demo.domain.chat.service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import umc.cockple.demo.domain.chat.events.ChatRoomRedisCleanupEvent;
import umc.cockple.demo.domain.chat.repository.redis.ChatListSubscriptionStore;
import umc.cockple.demo.domain.chat.service.websocket.ChatRoomListCacheService;
import umc.cockple.demo.domain.chat.repository.redis.ChatRoomSubscriptionStore;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatRoomRedisCleanupListener {

    private final ChatRoomListCacheService chatRoomListCacheService;
    private final ChatRoomSubscriptionStore chatRoomSubscriptionStore;
    private final ChatListSubscriptionStore chatListSubscriptionStore;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("chatExecutor")
    public void handleChatRoomRedisCleanup(ChatRoomRedisCleanupEvent event) {
        Long chatRoomId = event.chatRoomId();
        log.info("[채팅방 Redis 정리 시작] - chatRoomId: {}", chatRoomId);

        try {
            chatRoomListCacheService.evictLastMessage(chatRoomId);
        } catch (Exception e) {
            log.warn("[채팅방 Redis 정리] 마지막 메시지 캐시 best-effort 삭제 실패 - chatRoomId: {}", chatRoomId, e);
        }

        chatRoomSubscriptionStore.tryClearRoomSubscribers(chatRoomId);
        chatListSubscriptionStore.tryClearChatListSubscribers(chatRoomId);

        log.info("[채팅방 Redis 정리 완료] - chatRoomId: {}", chatRoomId);
    }
}
