package umc.cockple.demo.global.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 회원별 토큰 버전(tokenVersion)을 관리하는 저장소
 * 키가 존재하지 않으면 버전은 0으로 간주 (신규 회원은 별도 초기화 없이 0에서 시작)
 */
@Repository
@RequiredArgsConstructor
public class TokenVersionRepository {

    private static final String KEY_PREFIX = "member:tokenVersion:";

    private final StringRedisTemplate stringRedisTemplate;

    public long getVersion(Long memberId) {
        String value = stringRedisTemplate.opsForValue().get(KEY_PREFIX + memberId);
        return value == null ? 0L : Long.parseLong(value);
    }

    public long increment(Long memberId) {
        Long newVersion = stringRedisTemplate.opsForValue().increment(KEY_PREFIX + memberId);
        return newVersion == null ? 0L : newVersion;
    }
}
