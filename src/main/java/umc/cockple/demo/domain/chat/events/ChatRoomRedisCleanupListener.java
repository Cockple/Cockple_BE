package umc.cockple.demo.domain.chat.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import umc.cockple.demo.domain.chat.service.websocket.ChatListSubscriptionService;
import umc.cockple.demo.domain.chat.service.websocket.ChatRoomListCacheService;
import umc.cockple.demo.domain.chat.service.websocket.RedisSubscriptionService;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatRoomRedisCleanupListener {

    private final ChatRoomListCacheService chatRoomListCacheService;
    private final RedisSubscriptionService redisSubscriptionService;
    private final ChatListSubscriptionService chatListSubscriptionService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleChatRoomRedisCleanup(ChatRoomRedisCleanupEvent event) {
        Long chatRoomId = event.chatRoomId();
        log.info("[채팅방 Redis 정리 시작] - chatRoomId: {}", chatRoomId);

        try {
            chatRoomListCacheService.evictLastMessage(chatRoomId);
        } catch (Exception e) {
            log.warn("[채팅방 Redis 정리] 마지막 메시지 캐시 best-effort 삭제 실패 - chatRoomId: {}", chatRoomId, e);
        }

        redisSubscriptionService.tryClearRoomSubscribers(chatRoomId);
        chatListSubscriptionService.tryClearChatListSubscribers(chatRoomId);

        log.info("[채팅방 Redis 정리 완료] - chatRoomId: {}", chatRoomId);
    }
}
