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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoomSubscriptionStore 단위 테스트")
class ChatRoomSubscriptionStoreTest {

    @InjectMocks
    private ChatRoomSubscriptionStore chatRoomSubscriptionStore;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Nested
    @DisplayName("addSubscriber")
    class AddSubscriber {

        @Test
        @DisplayName("채팅방 구독 키에 멤버 ID를 저장하고 TTL을 갱신한다")
        void addsMemberId_andRefreshesTtl() {
            given(stringRedisTemplate.opsForSet()).willReturn(setOperations);

            chatRoomSubscriptionStore.addSubscriber(10L, 20L);

            verify(setOperations).add("chatroom:subscribers:10", "20");
            verify(stringRedisTemplate).expire("chatroom:subscribers:10", Duration.ofHours(2));
        }

        @Test
        @DisplayName("Redis 실패는 예외를 전파하지 않는다")
        void doesNotThrow_whenRedisFails() {
            given(stringRedisTemplate.opsForSet()).willReturn(setOperations);
            willThrow(new RuntimeException("redis down"))
                    .given(setOperations)
                    .add("chatroom:subscribers:10", "20");

            assertThatCode(() -> chatRoomSubscriptionStore.addSubscriber(10L, 20L))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("removeSubscriber")
    class RemoveSubscriber {

        @Test
        @DisplayName("남은 구독자가 없으면 채팅방 구독 키를 삭제한다")
        void deletesKey_whenNoSubscriberRemains() {
            given(stringRedisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.size("chatroom:subscribers:10")).willReturn(0L);

            chatRoomSubscriptionStore.removeSubscriber(10L, 20L);

            verify(setOperations).remove("chatroom:subscribers:10", "20");
            verify(stringRedisTemplate).delete("chatroom:subscribers:10");
            verify(stringRedisTemplate, never()).expire("chatroom:subscribers:10", Duration.ofHours(2));
        }

        @Test
        @DisplayName("남은 구독자가 있으면 채팅방 구독 키 TTL을 갱신한다")
        void refreshesTtl_whenSubscriberRemains() {
            given(stringRedisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.size("chatroom:subscribers:10")).willReturn(1L);

            chatRoomSubscriptionStore.removeSubscriber(10L, 20L);

            verify(setOperations).remove("chatroom:subscribers:10", "20");
            verify(stringRedisTemplate).expire("chatroom:subscribers:10", Duration.ofHours(2));
            verify(stringRedisTemplate, never()).delete("chatroom:subscribers:10");
        }

        @Test
        @DisplayName("Redis 실패는 예외를 전파하지 않는다")
        void doesNotThrow_whenRedisFails() {
            given(stringRedisTemplate.opsForSet()).willReturn(setOperations);
            willThrow(new RuntimeException("redis down"))
                    .given(setOperations)
                    .remove("chatroom:subscribers:10", "20");

            assertThatCode(() -> chatRoomSubscriptionStore.removeSubscriber(10L, 20L))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("getSubscribers")
    class GetSubscribers {

        @Test
        @DisplayName("키가 있으면 TTL을 갱신하고 멤버 ID를 Long으로 변환한다")
        void refreshesTtlAndReturnsParsedMemberIds_whenKeyExists() {
            given(stringRedisTemplate.hasKey("chatroom:subscribers:10")).willReturn(true);
            given(stringRedisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.members("chatroom:subscribers:10")).willReturn(Set.of("20", "30"));

            Set<Long> subscribers = chatRoomSubscriptionStore.getSubscribers(10L);

            assertThat(subscribers).containsExactlyInAnyOrder(20L, 30L);
            verify(stringRedisTemplate).expire("chatroom:subscribers:10", Duration.ofHours(2));
        }

        @Test
        @DisplayName("조회 결과가 비어 있으면 빈 Set을 반환한다")
        void returnsEmptySet_whenNoSubscribersExist() {
            given(stringRedisTemplate.hasKey("chatroom:subscribers:10")).willReturn(false);
            given(stringRedisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.members("chatroom:subscribers:10")).willReturn(Set.of());

            Set<Long> subscribers = chatRoomSubscriptionStore.getSubscribers(10L);

            assertThat(subscribers).isEmpty();
            verify(stringRedisTemplate, never()).expire("chatroom:subscribers:10", Duration.ofHours(2));
        }

        @Test
        @DisplayName("Redis 실패는 빈 Set을 반환한다")
        void returnsEmptySet_whenRedisFails() {
            given(stringRedisTemplate.hasKey("chatroom:subscribers:10"))
                    .willThrow(new RuntimeException("redis down"));

            Set<Long> subscribers = chatRoomSubscriptionStore.getSubscribers(10L);

            assertThat(subscribers).isEmpty();
        }
    }

    @Nested
    @DisplayName("tryClearRoomSubscribers")
    class TryClearRoomSubscribers {

        @Test
        @DisplayName("채팅방 구독 키는 best-effort로 삭제한다")
        void deletesKey() {
            chatRoomSubscriptionStore.tryClearRoomSubscribers(10L);

            verify(stringRedisTemplate).delete("chatroom:subscribers:10");
        }

        @Test
        @DisplayName("채팅방 구독 키 삭제 실패는 TTL 만료에 맡기고 예외를 전파하지 않는다")
        void doesNotThrow_whenRedisFails() {
            willThrow(new RuntimeException("redis down"))
                    .given(stringRedisTemplate)
                    .delete("chatroom:subscribers:10");

            assertThatCode(() -> chatRoomSubscriptionStore.tryClearRoomSubscribers(10L))
                    .doesNotThrowAnyException();
        }
    }
}
