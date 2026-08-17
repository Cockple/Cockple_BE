package umc.cockple.demo.domain.game.repository.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GameBoardSubscriber")
class GameBoardSubscriberTest {

    @Test
    @DisplayName("토큰으로 직렬화 후 역직렬화하면 원본 (memberId, sessionId)가 보존된다")
    void tokenRoundTrip() {
        GameBoardSubscriber original = new GameBoardSubscriber(42L, "ws-session-abc123");

        GameBoardSubscriber restored = GameBoardSubscriber.fromToken(original.toToken());

        assertThat(restored).isEqualTo(original);
    }

    @Test
    @DisplayName("sessionId에 구분자와 같은 문자가 있어도 첫 구분자 기준으로 memberId만 분리한다")
    void tokenSplitsOnFirstDelimiterOnly() {
        GameBoardSubscriber restored = GameBoardSubscriber.fromToken("42:a:b:c");

        assertThat(restored.memberId()).isEqualTo(42L);
        assertThat(restored.sessionId()).isEqualTo("a:b:c");
    }
}
