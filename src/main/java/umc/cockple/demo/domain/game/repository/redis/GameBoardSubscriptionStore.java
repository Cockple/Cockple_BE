package umc.cockple.demo.domain.game.repository.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 게임판 구독자(memberId) 집합을 Redis Set 으로 관리한다. (멀티 인스턴스 대응, chat 구독 방식 미러링)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GameBoardSubscriptionStore {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String GAME_BOARD_SUBSCRIBERS = "gameboard:subscribers:";
    private static final Duration SUBSCRIPTION_TTL = Duration.ofHours(2);

    public void addSubscriber(Long gameBoardId, Long memberId) {
        try {
            String key = GAME_BOARD_SUBSCRIBERS + gameBoardId;
            stringRedisTemplate.opsForSet().add(key, memberId.toString());
            stringRedisTemplate.expire(key, SUBSCRIPTION_TTL);
            log.info("게임판 구독 추가 - gameBoardId: {}, memberId: {}", gameBoardId, memberId);
        } catch (Exception e) {
            log.error("게임판 구독 추가 실패 - gameBoardId: {}, memberId: {}", gameBoardId, memberId, e);
        }
    }

    public void removeSubscriber(Long gameBoardId, Long memberId) {
        try {
            String key = GAME_BOARD_SUBSCRIBERS + gameBoardId;
            stringRedisTemplate.opsForSet().remove(key, memberId.toString());

            Long remaining = stringRedisTemplate.opsForSet().size(key);
            if (remaining != null && remaining == 0) {
                stringRedisTemplate.delete(key);
            } else {
                stringRedisTemplate.expire(key, SUBSCRIPTION_TTL);
            }
            log.info("게임판 구독 제거 - gameBoardId: {}, memberId: {}", gameBoardId, memberId);
        } catch (Exception e) {
            log.error("게임판 구독 제거 실패 - gameBoardId: {}, memberId: {}", gameBoardId, memberId, e);
        }
    }

    public Set<Long> getSubscribers(Long gameBoardId) {
        try {
            String key = GAME_BOARD_SUBSCRIBERS + gameBoardId;
            Set<String> members = stringRedisTemplate.opsForSet().members(key);
            if (members == null || members.isEmpty()) {
                return Set.of();
            }
            return members.stream().map(Long::parseLong).collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("게임판 구독자 조회 실패 - gameBoardId: {}", gameBoardId, e);
            return Set.of();
        }
    }
}
