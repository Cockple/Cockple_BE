package umc.cockple.demo.global.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import umc.cockple.demo.global.jwt.properties.JwtProperties;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private static final String KEY_PREFIX = "refresh:";
    private static final String CONSUMED_PREFIX = "refresh:consumed:";

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

    public Optional<Long> findAndDeleteByToken(String refreshToken) {
        String value = stringRedisTemplate.opsForValue().getAndDelete(KEY_PREFIX + refreshToken);
        if (value == null) return Optional.empty();
        return Optional.of(Long.valueOf(value));
    }

    public void delete(String refreshToken) {
        stringRedisTemplate.delete(KEY_PREFIX + refreshToken);
    }

    /**
     * 동시 재발급 경쟁을 재사용(탈취)이 아닌 정상 상황으로 식별하기 위한 마커
     */
    public void markConsumed(String refreshToken, Long memberId) {
        stringRedisTemplate.opsForValue().set(
                CONSUMED_PREFIX + refreshToken,
                String.valueOf(memberId),
                jwtProperties.getRefreshTokenReuseGrace(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * 해당 토큰이 grace window 이내에 정상 소비된 이력이 있는지 여부
     * true 이면 정상 경쟁/재시도, false 이면 grace 를 지난 재사용(탈취 의심)
     */
    public boolean isRecentlyConsumed(String refreshToken) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(CONSUMED_PREFIX + refreshToken));
    }
}
