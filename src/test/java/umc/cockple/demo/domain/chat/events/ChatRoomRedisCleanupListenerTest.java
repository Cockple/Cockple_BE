package umc.cockple.demo.domain.chat.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import umc.cockple.demo.domain.chat.service.websocket.ChatListSubscriptionService;
import umc.cockple.demo.domain.chat.service.websocket.ChatRoomListCacheService;
import umc.cockple.demo.domain.chat.service.websocket.RedisSubscriptionService;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoomRedisCleanupListener")
class ChatRoomRedisCleanupListenerTest {

    @InjectMocks
    private ChatRoomRedisCleanupListener listener;

    @Mock
    private ChatRoomListCacheService chatRoomListCacheService;
    @Mock
    private RedisSubscriptionService redisSubscriptionService;
    @Mock
    private ChatListSubscriptionService chatListSubscriptionService;

    @Test
    @DisplayName("채팅방 Redis 정리 이벤트는 커밋 이후 비동기로 처리되도록 설정한다")
    void handleChatRoomRedisCleanup_runsAfterCommit() throws NoSuchMethodException {
        Method method = ChatRoomRedisCleanupListener.class.getDeclaredMethod(
                "handleChatRoomRedisCleanup",
                ChatRoomRedisCleanupEvent.class
        );

        TransactionalEventListener annotation = method.getAnnotation(TransactionalEventListener.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(method.getAnnotation(Async.class)).isNotNull();
    }

    @Test
    @DisplayName("채팅방 Redis 정리 이벤트를 받으면 마지막 메시지 캐시와 구독 키를 삭제한다")
    void handleChatRoomRedisCleanup_clearsCacheAndSubscriptionKeys() {
        Long chatRoomId = 1L;
        ChatRoomRedisCleanupEvent event = ChatRoomRedisCleanupEvent.of(chatRoomId);

        listener.handleChatRoomRedisCleanup(event);

        var inOrder = inOrder(chatRoomListCacheService, redisSubscriptionService, chatListSubscriptionService);
        inOrder.verify(chatRoomListCacheService).evictLastMessage(chatRoomId);
        inOrder.verify(redisSubscriptionService).tryClearRoomSubscribers(chatRoomId);
        inOrder.verify(chatListSubscriptionService).tryClearChatListSubscribers(chatRoomId);
    }

    @Test
    @DisplayName("마지막 메시지 캐시 삭제가 실패해도 구독 키 정리를 계속한다")
    void handleChatRoomRedisCleanup_continuesWhenCacheEvictFails() {
        Long chatRoomId = 1L;
        willThrow(new RuntimeException("redis cache down"))
                .given(chatRoomListCacheService)
                .evictLastMessage(chatRoomId);

        listener.handleChatRoomRedisCleanup(ChatRoomRedisCleanupEvent.of(chatRoomId));

        verify(redisSubscriptionService).tryClearRoomSubscribers(chatRoomId);
        verify(chatListSubscriptionService).tryClearChatListSubscribers(chatRoomId);
    }
}
