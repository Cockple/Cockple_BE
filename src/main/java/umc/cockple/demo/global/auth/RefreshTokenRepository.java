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
}
