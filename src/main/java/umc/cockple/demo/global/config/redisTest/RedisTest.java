package umc.cockple.demo.global.config.redisTest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;


// 연결 테스트용이라 확인 했으면 클래스 날려도 됩니다 !
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisTest {

    private final StringRedisTemplate stringRedisTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void ping() {
        try {
            String pong = stringRedisTemplate.getRequiredConnectionFactory()
                    .getConnection()
                    .ping();
            log.info("Redis PING result: {}", pong);
        } catch (Exception e) {
            log.error("Redis connection failed", e);
        }
    }
}