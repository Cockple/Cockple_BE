package umc.cockple.demo.domain.game.service.websocket.broadcast;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.game.presentation.dto.GameBoardDTO;
import umc.cockple.demo.domain.game.realtime.GameRealtimeProtocol;
import umc.cockple.demo.domain.game.repository.redis.GameBoardSubscriber;
import umc.cockple.demo.domain.game.repository.redis.GameBoardSubscriptionStore;
import umc.cockple.demo.global.realtime.publish.RealtimeMessagePublisher;
import umc.cockple.demo.global.realtime.publish.RealtimePublishResult;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 게임판 변경을 그 게임판 구독자 전원에게 실시간 push 한다.
 * 구독 단위가 세션이므로, 구독 세션 각각을 겨냥해 발행한다.
 * 변경을 유발한 세션(excludedSessionId)은 이미 직접 응답을 받았으므로 제외한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GameBoardBroadcaster {

    private final GameBoardSubscriptionStore subscriptionStore;
    private final RealtimeMessagePublisher realtimeMessagePublisher;

    private final ConcurrentMap<Long, BoardPublication> boardPublications = new ConcurrentHashMap<>();

    public void broadcastBoardUpdate(
            Long gameBoardId, long revision, GameBoardDTO.Response board, String excludedSessionId) {
        BoardPublication publication = boardPublications.computeIfAbsent(gameBoardId, id -> new BoardPublication());
        synchronized (publication) {
            if (revision < publication.latestRevision) {
                return;
            }
            // 일부 세션 전송 후 실패해도 이전 보드가 다시 전송되지 않도록 먼저 기록한다.
            publication.latestRevision = revision;
            broadcastUpdate(gameBoardId, GameRealtimeProtocol.TYPE_BOARD_UPDATED, board.forBroadcast(), excludedSessionId);
        }
    }

    public void broadcastMembersUpdate(Long gameBoardId, Object membersData, String excludedSessionId) {
        broadcastUpdate(
                gameBoardId,
                GameRealtimeProtocol.TYPE_MEMBERS_UPDATED,
                membersData,
                excludedSessionId);
    }

    private void broadcastUpdate(
            Long gameBoardId,
            String type,
            Object data,
            String excludedSessionId) {
        Set<GameBoardSubscriber> subscribers = subscriptionStore.getSubscribers(gameBoardId);
        if (subscribers.isEmpty()) {
            log.info("게임판 {} 구독자가 없어 {} 브로드캐스트를 생략합니다.", gameBoardId, type);
            return;
        }

        int deliveredCount = 0;
        for (GameBoardSubscriber subscriber : subscribers) {
            if (subscriber.sessionId().equals(excludedSessionId)) {
                continue;
            }
            RealtimePublishResult result = realtimeMessagePublisher.publishToSession(
                    subscriber.memberId(),
                    subscriber.sessionId(),
                    GameRealtimeProtocol.DOMAIN,
                    type,
                    data);
            if (result.deliveredToAnySession()) {
                deliveredCount++;
            }
        }

        log.info("게임판 {} 브로드캐스트 완료 - gameBoardId: {}, 구독 세션: {}개, 전달: {}개",
                type, gameBoardId, subscribers.size(), deliveredCount);
    }

    private static final class BoardPublication {
        private long latestRevision;
    }
}
