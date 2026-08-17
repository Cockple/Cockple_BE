package umc.cockple.demo.domain.game.repository.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 게임판 구독자 집합을 Redis Set 으로 관리한다. (멀티 인스턴스 대응, chat 구독 방식 미러링)
 * 멤버당 다중 세션을 지원하므로 구독 단위는 세션이며, {@code memberId:sessionId} 토큰을 저장한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GameBoardSubscriptionStore {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String GAME_BOARD_SUBSCRIBERS = "gameboard:subscribers:";
    private static final Duration SUBSCRIPTION_TTL = Duration.ofHours(2);

    public void addSubscriber(Long gameBoardId, Long memberId, String sessionId) {
        try {
            String key = GAME_BOARD_SUBSCRIBERS + gameBoardId;
            stringRedisTemplate.opsForSet().add(key, new GameBoardSubscriber(memberId, sessionId).toToken());
            stringRedisTemplate.expire(key, SUBSCRIPTION_TTL);
            log.info("게임판 구독 추가 - gameBoardId: {}, memberId: {}, sessionId: {}", gameBoardId, memberId, sessionId);
        } catch (Exception e) {
            log.error("게임판 구독 추가 실패 - gameBoardId: {}, memberId: {}, sessionId: {}", gameBoardId, memberId, sessionId, e);
        }
    }

    public void removeSubscriber(Long gameBoardId, Long memberId, String sessionId) {
        try {
            String key = GAME_BOARD_SUBSCRIBERS + gameBoardId;
            stringRedisTemplate.opsForSet().remove(key, new GameBoardSubscriber(memberId, sessionId).toToken());

            Long remaining = stringRedisTemplate.opsForSet().size(key);
            if (remaining != null && remaining == 0) {
                stringRedisTemplate.delete(key);
            } else {
                stringRedisTemplate.expire(key, SUBSCRIPTION_TTL);
            }
            log.info("게임판 구독 제거 - gameBoardId: {}, memberId: {}, sessionId: {}", gameBoardId, memberId, sessionId);
        } catch (Exception e) {
            log.error("게임판 구독 제거 실패 - gameBoardId: {}, memberId: {}, sessionId: {}", gameBoardId, memberId, sessionId, e);
        }
    }

    public Set<GameBoardSubscriber> getSubscribers(Long gameBoardId) {
        try {
            String key = GAME_BOARD_SUBSCRIBERS + gameBoardId;
            Set<String> tokens = stringRedisTemplate.opsForSet().members(key);
            if (tokens == null || tokens.isEmpty()) {
                return Set.of();
            }
            return tokens.stream()
                    .map(GameBoardSubscriber::fromToken)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("게임판 구독자 조회 실패 - gameBoardId: {}", gameBoardId, e);
            return Set.of();
        }
    }
}
