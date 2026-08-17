package umc.cockple.demo.domain.game.service.websocket.broadcast;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.game.realtime.GameRealtimeProtocol;
import umc.cockple.demo.domain.game.repository.redis.GameBoardSubscriber;
import umc.cockple.demo.domain.game.repository.redis.GameBoardSubscriptionStore;
import umc.cockple.demo.global.realtime.publish.RealtimeMessagePublisher;
import umc.cockple.demo.global.realtime.publish.RealtimePublishResult;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameBoardBroadcaster")
class GameBoardBroadcasterTest {

    @Mock private GameBoardSubscriptionStore subscriptionStore;
    @Mock private RealtimeMessagePublisher realtimeMessagePublisher;

    @InjectMocks private GameBoardBroadcaster gameBoardBroadcaster;

    private static final Long BOARD_ID = 1L;
    private static final Object BOARD_DATA = new Object();

    @Test
    @DisplayName("변경을 일으킨 세션은 제외하고, 나머지 구독 세션 각각에 세션 단위로 발행한다")
    void broadcastPerSessionExcludingActor() {
        GameBoardSubscriber actor = new GameBoardSubscriber(10L, "session-actor");
        GameBoardSubscriber otherTabSameMember = new GameBoardSubscriber(10L, "session-other-tab");
        GameBoardSubscriber anotherMember = new GameBoardSubscriber(20L, "session-b");
        given(subscriptionStore.getSubscribers(BOARD_ID))
                .willReturn(Set.of(actor, otherTabSameMember, anotherMember));
        given(realtimeMessagePublisher.publishToSession(any(), any(), any(), any(), any()))
                .willReturn(new RealtimePublishResult(1, 1));

        gameBoardBroadcaster.broadcastBoardUpdate(BOARD_ID, BOARD_DATA, "session-actor");

        // 같은 회원의 다른 탭 세션에도 전달된다 (memberId가 아니라 sessionId로 제외하므로)
        then(realtimeMessagePublisher).should().publishToSession(
                eq(10L), eq("session-other-tab"),
                eq(GameRealtimeProtocol.DOMAIN), eq(GameRealtimeProtocol.TYPE_BOARD_UPDATED), eq(BOARD_DATA));
        then(realtimeMessagePublisher).should().publishToSession(
                eq(20L), eq("session-b"),
                eq(GameRealtimeProtocol.DOMAIN), eq(GameRealtimeProtocol.TYPE_BOARD_UPDATED), eq(BOARD_DATA));
        // 변경을 일으킨 세션에는 전달하지 않는다
        then(realtimeMessagePublisher).should(never()).publishToSession(
                eq(10L), eq("session-actor"), any(), any(), any());
    }

    @Test
    @DisplayName("구독 세션이 없으면 발행하지 않는다")
    void noSubscribersNoPublish() {
        given(subscriptionStore.getSubscribers(BOARD_ID)).willReturn(Set.of());

        gameBoardBroadcaster.broadcastBoardUpdate(BOARD_ID, BOARD_DATA, null);

        then(realtimeMessagePublisher).should(never()).publishToSession(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("excludedSessionId가 null이면(REST 경로) 모든 구독 세션에 발행한다")
    void restPathBroadcastsToAllSessions() {
        GameBoardSubscriber s1 = new GameBoardSubscriber(10L, "session-1");
        GameBoardSubscriber s2 = new GameBoardSubscriber(20L, "session-2");
        given(subscriptionStore.getSubscribers(BOARD_ID)).willReturn(Set.of(s1, s2));
        given(realtimeMessagePublisher.publishToSession(any(), any(), any(), any(), any()))
                .willReturn(new RealtimePublishResult(1, 1));

        gameBoardBroadcaster.broadcastBoardUpdate(BOARD_ID, BOARD_DATA, null);

        then(realtimeMessagePublisher).should().publishToSession(
                eq(10L), eq("session-1"), any(), any(), eq(BOARD_DATA));
        then(realtimeMessagePublisher).should().publishToSession(
                eq(20L), eq("session-2"), any(), any(), eq(BOARD_DATA));
    }
}
