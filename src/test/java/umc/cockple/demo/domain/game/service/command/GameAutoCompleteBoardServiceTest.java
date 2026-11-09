package umc.cockple.demo.domain.game.service.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import umc.cockple.demo.domain.game.domain.Court;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.events.GameBoardMembersChangedEvent;
import umc.cockple.demo.domain.game.repository.GameRepository;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.GameFixture;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameAutoCompleteBoardService")
class GameAutoCompleteBoardServiceTest {

    @Mock private GameRepository gameRepository;
    @Mock private GameBoardReader gameBoardReader;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private GameAutoCompleteBoardService gameAutoCompleteService;

    private static final Long BOARD_ID = 1L;

    @Test
    @DisplayName("시작 후 30분이 지난 진행 게임을 완료 처리하고 게임횟수를 증가시키며 보드 갱신 이벤트를 발행한다")
    void autoCompleteStaleGames_completesAndPublishes() {
        // given
        GameBoard board = GameFixture.gameBoard(BOARD_ID);
        Court court = GameFixture.court(10L, board, 1, "1번");
        GameBoardMember member = GameFixture.member(7L, board, "선수", Level.A);
        Game stale = GameFixture.playingGame(
                50L, board, court, LocalDateTime.now().minusMinutes(31),
                GameFixture.player(member, 0));
        given(gameRepository.findStaleGamesByGameBoardIdWithPlayers(eq(BOARD_ID), eq(GameStatus.PLAYING), any()))
                .willReturn(List.of(stale));
        given(gameBoardReader.readForUpdate(BOARD_ID)).willReturn(board);

        // when
        int completed = gameAutoCompleteService.completeStaleGames(BOARD_ID, LocalDateTime.now().minusMinutes(30));

        // then
        assertThat(completed).isEqualTo(1);
        assertThat(board.getRevision()).isEqualTo(1L);
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(gameBoardReader, gameRepository);
        order.verify(gameBoardReader).readForUpdate(BOARD_ID);
        order.verify(gameRepository).findStaleGamesByGameBoardIdWithPlayers(eq(BOARD_ID), eq(GameStatus.PLAYING), any());
        assertThat(stale.getStatus()).isEqualTo(GameStatus.COMPLETED);
        assertThat(stale.getCompletedAt()).isNotNull();
        assertThat(member.getGameCount()).isEqualTo(1);
        then(eventPublisher).should()
                .publishEvent(GameBoardMembersChangedEvent.membersAndBoard(BOARD_ID, null));
    }

    @Test
    @DisplayName("자동 완료 대상 게임이 없으면 아무것도 하지 않고 0을 반환한다")
    void autoCompleteStaleGames_noStaleGames() {
        given(gameRepository.findStaleGamesByGameBoardIdWithPlayers(eq(BOARD_ID), eq(GameStatus.PLAYING), any()))
                .willReturn(List.of());

        GameBoard board = GameFixture.gameBoard(BOARD_ID);
        given(gameBoardReader.readForUpdate(BOARD_ID)).willReturn(board);
        int completed = gameAutoCompleteService.completeStaleGames(BOARD_ID, LocalDateTime.now().minusMinutes(30));

        assertThat(completed).isZero();
        assertThat(board.getRevision()).isZero();
        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    void completionGuardPreservesCompletedAt() {
        Game game = Game.builder().status(GameStatus.PLAYING).build();
        LocalDateTime completedAt = LocalDateTime.now().minusMinutes(1);
        game.complete(completedAt);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> game.complete(LocalDateTime.now()))
                .isInstanceOf(umc.cockple.demo.domain.game.exception.GameException.class);
        assertThat(game.getCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    void completionGuardRejectsWaitingGame() {
        Game game = Game.builder().status(GameStatus.WAITING).build();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> game.complete(LocalDateTime.now()))
                .isInstanceOf(umc.cockple.demo.domain.game.exception.GameException.class);
        assertThat(game.getStatus()).isEqualTo(GameStatus.WAITING);
        assertThat(game.getCompletedAt()).isNull();
    }
}
