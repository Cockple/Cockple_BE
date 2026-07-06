package umc.cockple.demo.global.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.member.repository.MemberRepository;

@Repository
@RequiredArgsConstructor
public class TokenVersionRepository {

    private static final String KEY_PREFIX = "member:tokenVersion:";

    private final StringRedisTemplate stringRedisTemplate;
    private final MemberRepository memberRepository;

    /**
     * 현재 토큰 버전을 반환한다.
     * 캐시 hit 시 Redis, miss 시 DB에서 읽어 재적재
     */
    public long getVersion(Long memberId) {
        String cached = stringRedisTemplate.opsForValue().get(KEY_PREFIX + memberId);
        if (cached != null) {
            return Long.parseLong(cached);
        }
        long version = memberRepository.findTokenVersionById(memberId).orElse(0L);
        cache(memberId, version);
        return version;
    }

    /**
     * 토큰 버전을 1 증가시키고 새 값을 반환
     */
    @Transactional
    public long increment(Long memberId) {
        memberRepository.incrementTokenVersion(memberId);
        long newVersion = memberRepository.findTokenVersionById(memberId).orElse(0L);
        cache(memberId, newVersion);
        return newVersion;
    }

    private void cache(Long memberId, long version) {
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + memberId, String.valueOf(version));
    }
}
