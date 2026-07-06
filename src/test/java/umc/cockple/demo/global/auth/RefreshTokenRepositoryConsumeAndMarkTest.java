package umc.cockple.demo.global.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import umc.cockple.demo.support.IntegrationTestBase;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RefreshTokenRepository.consumeAndMark - GETDEL+소비마커 원자 스크립트")
class RefreshTokenRepositoryConsumeAndMarkTest extends IntegrationTestBase {

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("활성 토큰을 소비하면 memberId 반환 + 활성키 삭제 + 소비 마커 기록을 원자적으로 수행한다")
    void consumeActiveToken() {
        String token = "rt-" + UUID.randomUUID();
        refreshTokenRepository.save(token, 42L);

        Optional<Long> result = refreshTokenRepository.consumeAndMark(token);

        assertThat(result).contains(42L);
        // 활성 키가 삭제되어 두 번째 소비는 empty (GETDEL 의미)
        assertThat(refreshTokenRepository.consumeAndMark(token)).isEmpty();
        // 삭제와 동시에 소비 마커가 원자적으로 기록됨 (동시 재발급 오탐 방지의 핵심)
        assertThat(refreshTokenRepository.isRecentlyConsumed(token)).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 토큰이면 empty 반환 + 소비 마커도 남기지 않는다")
    void consumeMissingToken() {
        String token = "rt-" + UUID.randomUUID();

        Optional<Long> result = refreshTokenRepository.consumeAndMark(token);

        assertThat(result).isEmpty();
        assertThat(refreshTokenRepository.isRecentlyConsumed(token)).isFalse();
    }
}
