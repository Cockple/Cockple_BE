package umc.cockple.demo.domain.chat.repository.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatListSubscriptionStore 단위 테스트")
class ChatListSubscriptionStoreTest {

    @InjectMocks
    private ChatListSubscriptionStore chatListSubscriptionStore;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Nested
    @DisplayName("subscribeToChatList")
    class SubscribeToChatList {

        @Test
        @DisplayName("요청한 모든 채팅방 목록 구독 키에 멤버 ID를 저장하고 TTL을 갱신한다")
        void addsMemberIdToAllRooms_andRefreshesTtl() {
            given(stringRedisTemplate.opsForSet()).willReturn(setOperations);

            chatListSubscriptionStore.subscribeToChatList(10L, List.of(20L, 30L));

            verify(setOperations).add("chatlist:subscribers:20", "10");
            verify(setOperations).add("chatlist:subscribers:30", "10");
            verify(stringRedisTemplate).expire("chatlist:subscribers:20", Duration.ofHours(2));
            verify(stringRedisTemplate).expire("chatlist:subscribers:30", Duration.ofHours(2));
        }

        @Test
        @DisplayName("Redis 실패는 예외를 전파하지 않는다")
        void doesNotThrow_whenRedisFails() {
            given(stringRedisTemplate.opsForSet()).willReturn(setOperations);
            willThrow(new RuntimeException("redis down"))
                    .given(setOperations)
                    .add("chatlist:subscribers:20", "10");

            assertThatCode(() -> chatListSubscriptionStore.subscribeToChatList(10L, List.of(20L, 30L)))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("unsubscribeFromChatList")
    class UnsubscribeFromChatList {

        @Test
        @DisplayName("요청한 모든 채팅방 목록 구독 키에서 멤버 ID를 제거한다")
        void removesMemberIdFromAllRooms() {
            given(stringRedisTemplate.opsForSet()).willReturn(setOperations);

            chatListSubscriptionStore.unsubscribeFromChatList(10L, List.of(20L, 30L));

            verify(setOperations).remove("chatlist:subscribers:20", "10");
            verify(setOperations).remove("chatlist:subscribers:30", "10");
        }

        @Test
        @DisplayName("Redis 실패는 예외를 전파하지 않는다")
        void doesNotThrow_whenRedisFails() {
            given(stringRedisTemplate.opsForSet()).willReturn(setOperations);
            willThrow(new RuntimeException("redis down"))
                    .given(setOperations)
                    .remove("chatlist:subscribers:20", "10");

            assertThatCode(() -> chatListSubscriptionStore.unsubscribeFromChatList(10L, List.of(20L, 30L)))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("getChatListSubscribers")
    class GetChatListSubscribers {

        @Test
        @DisplayName("멤버 ID를 Long으로 변환해 반환한다")
        void returnsParsedMemberIds() {
            given(stringRedisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.members("chatlist:subscribers:20")).willReturn(Set.of("10", "30"));

            Set<Long> subscribers = chatListSubscriptionStore.getChatListSubscribers(20L);

            assertThat(subscribers).containsExactlyInAnyOrder(10L, 30L);
        }

        @Test
        @DisplayName("조회 결과가 비어 있으면 빈 Set을 반환한다")
        void returnsEmptySet_whenNoSubscribersExist() {
            given(stringRedisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.members("chatlist:subscribers:20")).willReturn(Set.of());

            Set<Long> subscribers = chatListSubscriptionStore.getChatListSubscribers(20L);

            assertThat(subscribers).isEmpty();
        }

        @Test
        @DisplayName("조회 결과가 null이면 빈 Set을 반환한다")
        void returnsEmptySet_whenRedisReturnsNull() {
            given(stringRedisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.members("chatlist:subscribers:20")).willReturn(null);

            Set<Long> subscribers = chatListSubscriptionStore.getChatListSubscribers(20L);

            assertThat(subscribers).isEmpty();
        }

        @Test
        @DisplayName("Redis 실패는 빈 Set을 반환한다")
        void returnsEmptySet_whenRedisFails() {
            given(stringRedisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.members("chatlist:subscribers:20"))
                    .willThrow(new RuntimeException("redis down"));

            Set<Long> subscribers = chatListSubscriptionStore.getChatListSubscribers(20L);

            assertThat(subscribers).isEmpty();
        }
    }

    @Nested
    @DisplayName("tryClearChatListSubscribers")
    class TryClearChatListSubscribers {

        @Test
        @DisplayName("채팅 목록 구독 키는 best-effort로 삭제한다")
        void deletesKey() {
            chatListSubscriptionStore.tryClearChatListSubscribers(10L);

            verify(stringRedisTemplate).delete("chatlist:subscribers:10");
        }

        @Test
        @DisplayName("채팅 목록 구독 키 삭제 실패는 TTL 만료에 맡기고 예외를 전파하지 않는다")
        void doesNotThrow_whenRedisFails() {
            willThrow(new RuntimeException("redis down"))
                    .given(stringRedisTemplate)
                    .delete("chatlist:subscribers:10");

            assertThatCode(() -> chatListSubscriptionStore.tryClearChatListSubscribers(10L))
                    .doesNotThrowAnyException();
        }
    }
}
