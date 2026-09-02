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
@DisplayName("GameAutoCompleteService")
class GameAutoCompleteServiceTest {

    @Mock private GameRepository gameRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private GameAutoCompleteService gameAutoCompleteService;

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
        given(gameRepository.findByStatusAndStartedAtBeforeWithPlayers(eq(GameStatus.PLAYING), any()))
                .willReturn(List.of(stale));

        // when
        int completed = gameAutoCompleteService.autoCompleteStaleGames();

        // then
        assertThat(completed).isEqualTo(1);
        assertThat(stale.getStatus()).isEqualTo(GameStatus.COMPLETED);
        assertThat(stale.getCompletedAt()).isNotNull();
        assertThat(member.getGameCount()).isEqualTo(1);
        then(eventPublisher).should()
                .publishEvent(GameBoardMembersChangedEvent.membersAndBoard(BOARD_ID, null));
    }

    @Test
    @DisplayName("자동 완료 대상 게임이 없으면 아무것도 하지 않고 0을 반환한다")
    void autoCompleteStaleGames_noStaleGames() {
        given(gameRepository.findByStatusAndStartedAtBeforeWithPlayers(eq(GameStatus.PLAYING), any()))
                .willReturn(List.of());

        int completed = gameAutoCompleteService.autoCompleteStaleGames();

        assertThat(completed).isZero();
        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    @DisplayName("여러 게임판에 걸쳐 있으면 게임판마다 한 번씩 이벤트를 발행한다")
    void autoCompleteStaleGames_publishesOncePerBoard() {
        // given - 같은 게임판의 두 게임 + 다른 게임판의 한 게임
        GameBoard board1 = GameFixture.gameBoard(BOARD_ID);
        GameBoard board2 = GameFixture.gameBoard(2L);
        Court court1 = GameFixture.court(10L, board1, 1, "1번");
        Court court2 = GameFixture.court(11L, board1, 2, "2번");
        Court court3 = GameFixture.court(12L, board2, 1, "1번");
        Game g1 = GameFixture.playingGame(50L, board1, court1, LocalDateTime.now().minusMinutes(31),
                GameFixture.player(GameFixture.member(1L, board1, "a", Level.A), 0));
        Game g2 = GameFixture.playingGame(51L, board1, court2, LocalDateTime.now().minusMinutes(40),
                GameFixture.player(GameFixture.member(2L, board1, "b", Level.A), 0));
        Game g3 = GameFixture.playingGame(52L, board2, court3, LocalDateTime.now().minusMinutes(31),
                GameFixture.player(GameFixture.member(3L, board2, "c", Level.A), 0));
        given(gameRepository.findByStatusAndStartedAtBeforeWithPlayers(eq(GameStatus.PLAYING), any()))
                .willReturn(List.of(g1, g2, g3));

        // when
        int completed = gameAutoCompleteService.autoCompleteStaleGames();

        // then
        assertThat(completed).isEqualTo(3);
        then(eventPublisher).should()
                .publishEvent(GameBoardMembersChangedEvent.membersAndBoard(BOARD_ID, null));
        then(eventPublisher).should()
                .publishEvent(GameBoardMembersChangedEvent.membersAndBoard(2L, null));
    }
}
