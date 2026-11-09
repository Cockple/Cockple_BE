package umc.cockple.demo.domain.game.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GameStatus")
class GameStatusTest {

    @Test
    @DisplayName("WAITING과 PLAYING은 활성 상태이고 COMPLETED는 활성 상태가 아니다")
    void isActive() {
        assertThat(GameStatus.WAITING.isActive()).isTrue();
        assertThat(GameStatus.PLAYING.isActive()).isTrue();
        assertThat(GameStatus.COMPLETED.isActive()).isFalse();
    }

    @Test
    @DisplayName("활성 상태 목록은 isActive가 true인 상태와 일치한다")
    void activeStatuses_matchIsActive() {
        assertThat(GameStatus.ACTIVE_STATUSES)
                .containsExactlyInAnyOrder(GameStatus.WAITING, GameStatus.PLAYING);
    }
}
