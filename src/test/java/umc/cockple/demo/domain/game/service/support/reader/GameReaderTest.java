package umc.cockple.demo.domain.game.service.support.reader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.service.GamePairCount;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.repository.GameRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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

    @Test
    @DisplayName("명단이 주어진 상태의 게임에 포함됐는지 조회한다")
    void existsByGameBoardMemberAndStatuses_delegatesToRepository() {
        Long gameBoardMemberId = 2L;
        List<GameStatus> statuses = List.of(GameStatus.PLAYING, GameStatus.WAITING);
        given(gameRepository.existsByGameBoardMemberIdAndStatusIn(gameBoardMemberId, statuses))
                .willReturn(true);

        assertThat(gameReader.existsByGameBoardMemberAndStatuses(gameBoardMemberId, statuses)).isTrue();
        then(gameRepository).should()
                .existsByGameBoardMemberIdAndStatusIn(gameBoardMemberId, statuses);
    }

    @Test
    @DisplayName("완료 경기의 대상 멤버 페어 집계를 조회한다")
    void readCompletedPairCounts_delegatesToRepository() {
        List<Long> gameBoardMemberIds = List.of(10L, 20L);
        List<GamePairCount> pairCounts = List.of(new GamePairCount(10L, 20L, 3));
        given(gameRepository.countCompletedGamePairs(GAME_BOARD_ID, gameBoardMemberIds))
                .willReturn(pairCounts);

        assertThat(gameReader.readCompletedPairCounts(GAME_BOARD_ID, gameBoardMemberIds))
                .containsExactlyElementsOf(pairCounts);
    }

    @Test
    @DisplayName("가장 최근 완료 경기의 멤버 ID를 조회한다")
    void readLatestCompletedGameMemberIds_delegatesToRepository() {
        List<Long> memberIds = List.of(10L, 20L, 30L, 40L);
        given(gameRepository.findLatestCompletedGameMemberIds(GAME_BOARD_ID))
                .willReturn(memberIds);

        assertThat(gameReader.readLatestCompletedGameMemberIds(GAME_BOARD_ID))
                .containsExactlyElementsOf(memberIds);
    }
}
