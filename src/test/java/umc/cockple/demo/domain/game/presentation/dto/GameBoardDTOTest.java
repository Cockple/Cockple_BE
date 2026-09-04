package umc.cockple.demo.domain.game.presentation.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import umc.cockple.demo.domain.game.enums.CourtStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GameBoardDTO.Response")
class GameBoardDTOTest {

    @Test
    @DisplayName("forBroadcast는 개인화 값 isGameHost를 false로 내리고 나머지 필드는 유지한다")
    void forBroadcast_neutralizesGameHostFlag() {
        GameBoardDTO.CourtInfo court = new GameBoardDTO.CourtInfo(10L, 1, "1번", CourtStatus.EMPTY, null);
        GameBoardDTO.WaitingInfo waiting = new GameBoardDTO.WaitingInfo(50L, 1, List.of());
        GameBoardDTO.Response original = new GameBoardDTO.Response(true, 1, List.of(court), List.of(waiting));

        GameBoardDTO.Response broadcast = original.forBroadcast();

        assertThat(broadcast.isGameHost()).isFalse();
        assertThat(broadcast.courtCount()).isEqualTo(1);
        assertThat(broadcast.courts()).isEqualTo(original.courts());
        assertThat(broadcast.waitings()).isEqualTo(original.waitings());
    }
}
