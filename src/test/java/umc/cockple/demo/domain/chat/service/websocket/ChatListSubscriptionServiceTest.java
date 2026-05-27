package umc.cockple.demo.domain.chat.service.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatListSubscriptionService 단위 테스트")
class ChatListSubscriptionServiceTest {

    @InjectMocks
    private ChatListSubscriptionService chatListSubscriptionService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("채팅 목록 구독 키는 best-effort로 삭제한다")
    void tryClearChatListSubscribers_deletesKey() {
        chatListSubscriptionService.tryClearChatListSubscribers(10L);

        verify(stringRedisTemplate).delete("chatlist:subscribers:10");
    }

    @Test
    @DisplayName("채팅 목록 구독 키 삭제 실패는 TTL 만료에 맡기고 예외를 전파하지 않는다")
    void tryClearChatListSubscribers_doesNotThrowWhenRedisFails() {
        willThrow(new RuntimeException("redis down"))
                .given(stringRedisTemplate)
                .delete("chatlist:subscribers:10");

        assertThatCode(() -> chatListSubscriptionService.tryClearChatListSubscribers(10L))
                .doesNotThrowAnyException();
    }
}
