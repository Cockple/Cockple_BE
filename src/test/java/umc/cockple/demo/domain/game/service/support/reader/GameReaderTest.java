package umc.cockple.demo.domain.game.service.support.reader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.repository.GameRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameReader")
class GameReaderTest {

    private static final Long GAME_BOARD_ID = 1L;

    @InjectMocks private GameReader gameReader;
    @Mock private GameRepository gameRepository;
    @Mock private Game game;

    @Test
    @DisplayName("게임판과 상태 조건으로 플레이어를 포함한 게임을 조회한다")
    void readAllByGameBoardAndStatuses_delegatesToRepository() {
        List<GameStatus> statuses = List.of(GameStatus.PLAYING, GameStatus.WAITING);
        given(gameRepository.findByGameBoardIdAndStatusInWithPlayers(GAME_BOARD_ID, statuses))
                .willReturn(List.of(game));

        assertThat(gameReader.readAllByGameBoardAndStatuses(GAME_BOARD_ID, statuses))
                .containsExactly(game);
    }
}
