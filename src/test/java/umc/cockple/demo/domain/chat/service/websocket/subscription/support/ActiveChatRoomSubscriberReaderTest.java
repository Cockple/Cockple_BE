package umc.cockple.demo.domain.chat.service.websocket.subscription.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.repository.redis.ChatRoomSubscriptionStore;
import umc.cockple.demo.domain.chat.service.websocket.session.ChatSessionRegistry;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActiveChatRoomSubscriberReader")
class ActiveChatRoomSubscriberReaderTest {

    @Mock private ChatRoomSubscriptionStore chatRoomSubscriptionStore;
    @Mock private ChatSessionRegistry sessionRegistry;

    private ActiveChatRoomSubscriberReader activeChatRoomSubscriberReader;

    @BeforeEach
    void setUp() {
        activeChatRoomSubscriberReader = new ActiveChatRoomSubscriberReader(
                chatRoomSubscriptionStore,
                sessionRegistry
        );
    }

    @Test
    @DisplayName("Redis 구독자 중 열린 세션 멤버만 활성 구독자로 반환한다")
    void findActiveSubscribers_returnsOpenSubscribers() {
        // given
        Long chatRoomId = 1L;
        Set<Long> subscribedMemberIds = Set.of(10L, 20L);
        given(chatRoomSubscriptionStore.getSubscribers(chatRoomId)).willReturn(subscribedMemberIds);
        given(sessionRegistry.findOpenMemberIds(subscribedMemberIds)).willReturn(List.of(10L));

        // when
        List<Long> activeSubscribers = activeChatRoomSubscriberReader.findActiveSubscribers(chatRoomId);

        // then
        assertThat(activeSubscribers).containsExactly(10L);
    }
}
