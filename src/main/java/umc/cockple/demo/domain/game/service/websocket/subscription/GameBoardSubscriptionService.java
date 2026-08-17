package umc.cockple.demo.domain.game.service.websocket.subscription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import umc.cockple.demo.domain.game.repository.redis.GameBoardSubscriptionStore;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;

/**
 * 게임판 구독/구독 해제. 구독한 멤버는 이후 코트 변경 등 라이브 업데이트를 push 받는다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameBoardSubscriptionService {

    private final GameBoardReader gameBoardReader;
    private final GameBoardSubscriptionStore subscriptionStore;

    public void subscribe(Long gameBoardId, Long memberId, String sessionId) {
        gameBoardReader.read(gameBoardId); // 존재 검증 (없으면 GAME_BOARD_NOT_FOUND)
        subscriptionStore.addSubscriber(gameBoardId, memberId, sessionId);
    }

    public void unsubscribe(Long gameBoardId, Long memberId, String sessionId) {
        subscriptionStore.removeSubscriber(gameBoardId, memberId, sessionId);
    }
}
