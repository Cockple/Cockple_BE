package umc.cockple.demo.domain.game.service.websocket.broadcast;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.game.realtime.GameRealtimeProtocol;
import umc.cockple.demo.domain.game.repository.redis.GameBoardSubscriptionStore;
import umc.cockple.demo.global.realtime.publish.RealtimeMessagePublisher;
import umc.cockple.demo.global.realtime.publish.RealtimePublishResult;

import java.util.Set;

/**
 * 게임판 변경을 그 게임판 구독자 전원에게 실시간 push 한다.
 * 공용 발행기는 memberId 단위라, 구독자 집합을 순회하며 각자에게 발행한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GameBoardBroadcaster {

    private final GameBoardSubscriptionStore subscriptionStore;
    private final RealtimeMessagePublisher realtimeMessagePublisher;

    public void broadcastBoardUpdate(Long gameBoardId, Object boardData, Long excludedMemberId) {
        Set<Long> subscribers = subscriptionStore.getSubscribers(gameBoardId);
        if (subscribers.isEmpty()) {
            log.info("게임판 {} 구독자가 없어 브로드캐스트를 생략합니다.", gameBoardId);
            return;
        }

        int deliveredCount = 0;
        for (Long memberId : subscribers) {
            if (memberId.equals(excludedMemberId)) {
                continue;
            }
            RealtimePublishResult result = realtimeMessagePublisher.publish(
                    memberId,
                    GameRealtimeProtocol.DOMAIN,
                    GameRealtimeProtocol.TYPE_BOARD_UPDATED,
                    boardData);
            if (result.deliveredToAnySession()) {
                deliveredCount++;
            }
        }

        log.info("게임판 브로드캐스트 완료 - gameBoardId: {}, 구독자: {}명, 전달: {}명",
                gameBoardId, subscribers.size(), deliveredCount);
    }
}
