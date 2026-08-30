package umc.cockple.demo.domain.game.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GameErrorCode")
class GameErrorCodeTest {

    @Test
    @DisplayName("게임 도메인 오류 코드는 서로 고유하다")
    void errorCodes_areUnique() {
        assertThat(Arrays.stream(GameErrorCode.values()).map(GameErrorCode::getCode))
                .doesNotHaveDuplicates();
    }
}
