package umc.cockple.demo.global.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;
import umc.cockple.demo.global.jwt.properties.JwtProperties;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private static final String KEY_PREFIX = "refresh:";
    private static final String CONSUMED_PREFIX = "refresh:consumed:";

    /**.
     * KEYS[1]=활성키, KEYS[2]=소비 마커키, ARGV[1]=grace TTL(ms)
     * @return memberId 또는 nil.
     */
    private static final RedisScript<String> CONSUME_AND_MARK_SCRIPT = RedisScript.of("""
            local memberId = redis.call('GET', KEYS[1])
            if not memberId then
                return nil
            end
            redis.call('DEL', KEYS[1])
            redis.call('SET', KEYS[2], memberId, 'PX', ARGV[1])
            return memberId
            """, String.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtProperties jwtProperties;

    public void save(String refreshToken, Long memberId) {
        stringRedisTemplate.opsForValue().set(
                KEY_PREFIX + refreshToken,
                String.valueOf(memberId),
                jwtProperties.getRefreshTokenValidity(),
                TimeUnit.MILLISECONDS
        );
    }

    public Optional<Long> consumeAndMark(String refreshToken) {
        String memberId = stringRedisTemplate.execute(
                CONSUME_AND_MARK_SCRIPT,
                List.of(KEY_PREFIX + refreshToken, CONSUMED_PREFIX + refreshToken),
                String.valueOf(jwtProperties.getRefreshTokenReuseGrace())
        );
        return memberId == null ? Optional.empty() : Optional.of(Long.valueOf(memberId));
    }

    public void delete(String refreshToken) {
        stringRedisTemplate.delete(KEY_PREFIX + refreshToken);
    }

    /**
     * 해당 토큰이 grace window 이내에 정상 소비된 이력이 있는지 여부
     * true 이면 정상 경쟁/재시도, false 이면 grace 를 지난 재사용(탈취 의심)
     */
    public boolean isRecentlyConsumed(String refreshToken) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(CONSUMED_PREFIX + refreshToken));
    }
}
