package umc.cockple.demo.global.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.fixture.MemberFixture;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TokenVersionRepository - DB(SoT) fallback + Redis 캐시")
class TokenVersionRepositoryDbFallbackTest extends IntegrationTestBase {

    private static final String KEY_PREFIX = "member:tokenVersion:";

    @Autowired TokenVersionRepository tokenVersionRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired StringRedisTemplate redis;

    private Long memberId;

    @AfterEach
    void tearDown() {
        if (memberId != null) {
            redis.delete(KEY_PREFIX + memberId);
            memberRepository.deleteById(memberId);
        }
    }

    @Test
    @DisplayName("Redis 캐시 miss여도 DB의 token_version을 읽어 반환하고 캐시에 재적재한다")
    void getVersionFallsBackToDbAndRepopulatesCache() {
        Member member = memberRepository.save(
                MemberFixture.createMember("버전회원", Gender.MALE, Level.A, 990001L));
        memberId = member.getId();

        // 무효화 발생 -> DB=1, Redis=1
        tokenVersionRepository.increment(memberId);
        // Redis 키가 eviction/유실된 상황 재현
        redis.delete(KEY_PREFIX + memberId);

        long version = tokenVersionRepository.getVersion(memberId);

        // 기존 코드였다면 miss=0으로 오판했을 값을 DB(SoT)에서 1로 복구
        assertThat(version).isEqualTo(1L);
        // 캐시에 재적재됨
        assertThat(redis.opsForValue().get(KEY_PREFIX + memberId)).isEqualTo("1");
    }

    @Test
    @DisplayName("increment는 DB를 원자적으로 올리고 캐시도 갱신한다")
    void incrementUpdatesDbAndCache() {
        Member member = memberRepository.save(
                MemberFixture.createMember("증가회원", Gender.MALE, Level.A, 990002L));
        memberId = member.getId();

        long v1 = tokenVersionRepository.increment(memberId);
        long v2 = tokenVersionRepository.increment(memberId);

        assertThat(v1).isEqualTo(1L);
        assertThat(v2).isEqualTo(2L);
        assertThat(redis.opsForValue().get(KEY_PREFIX + memberId)).isEqualTo("2");
    }
}
