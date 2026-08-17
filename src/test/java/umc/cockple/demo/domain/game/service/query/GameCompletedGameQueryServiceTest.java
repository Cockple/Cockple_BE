package umc.cockple.demo.domain.game.service.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import umc.cockple.demo.domain.game.domain.Court;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.repository.GameRepository;
import umc.cockple.demo.domain.game.service.query.result.GameCompletedGameResult;
import umc.cockple.demo.domain.game.service.query.result.GameCompletedGameResult.CompletedGameView;
import umc.cockple.demo.domain.game.service.query.result.GameCompletedGameResult.PlayerView;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.GameFixture;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameCompletedGameQueryService")
class GameCompletedGameQueryServiceTest {

    @Mock private GameBoardReader gameBoardReader;
    @Mock private GameRepository gameRepository;

    @InjectMocks private GameCompletedGameQueryService gameCompletedGameQueryService;

    private static final Long MEMBER_ID = 100L;
    private static final Long BOARD_ID = 1L;
    private GameBoard board;

    @BeforeEach
    void setUp() {
        board = GameFixture.gameBoard(BOARD_ID);
    }

    @Test
    @DisplayName("커서 페이지네이션: size+1개면 hasNext=true, size만 반환하고 nextCursor는 '완료시각_id', 완료 시각 오름차순")
    void getCompletedGames_paginatesWithCompositeCursor() {
        // given
        Court court1 = GameFixture.court(10L, board, 1, "1번 코트");
        GameBoardMember m1 = GameFixture.member(7L, board, "선수A", Level.A);
        GameBoardMember m2 = GameFixture.member(8L, board, "선수B", Level.SEMI_EXPERT);
        LocalDateTime base = LocalDateTime.now().minusMinutes(30);
        LocalDateTime completed50 = base.plusMinutes(10);
        LocalDateTime completed51 = base.plusMinutes(15);
        Game g50 = GameFixture.completedGame(50L, board, court1, base, completed50,
                GameFixture.player(m1, 0), GameFixture.player(m2, 1));
        Game g51 = GameFixture.completedGame(51L, board, court1, base, completed51,
                GameFixture.player(m1, 0));

        given(gameBoardReader.read(BOARD_ID)).willReturn(board);
        // 첫 페이지(courtNo/cursor 없음), size=2 → size+1=3개 조회되어 hasNext
        given(gameRepository.findCompletedGameIds(
                eq(BOARD_ID), eq(GameStatus.COMPLETED), isNull(), isNull(), isNull(), any(Pageable.class)))
                .willReturn(List.of(50L, 51L, 52L));
        given(gameRepository.findByIdInWithPlayers(List.of(50L, 51L)))
                .willReturn(List.of(g50, g51));

        // when
        GameCompletedGameResult result = gameCompletedGameQueryService
                .getCompletedGames(MEMBER_ID, BOARD_ID, null, null, 2);

        // then
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(completed51 + "_51"); // 마지막 게임의 완료시각_id
        assertThat(result.games()).extracting(CompletedGameView::gameId).containsExactly(50L, 51L);

        CompletedGameView first = result.games().get(0);
        assertThat(first.courtNo()).isEqualTo(1);
        assertThat(first.completedAt()).isEqualTo(completed50);
        assertThat(first.durationMin()).isEqualTo(10); // completedAt - startedAt
        assertThat(first.players()).extracting(PlayerView::gameBoardMemberId).containsExactly(7L, 8L);
        assertThat(first.players().get(0).level()).isEqualTo(Level.A);
    }

    @Test
    @DisplayName("완료 게임이 size 이하이면 hasNext=false, nextCursor=null")
    void getCompletedGames_lastPage() {
        Court court1 = GameFixture.court(10L, board, 1, "1번 코트");
        GameBoardMember m1 = GameFixture.member(7L, board, "선수A", Level.A);
        LocalDateTime base = LocalDateTime.now().minusMinutes(10);
        Game g50 = GameFixture.completedGame(50L, board, court1, base, base.plusMinutes(5),
                GameFixture.player(m1, 0));

        given(gameBoardReader.read(BOARD_ID)).willReturn(board);
        given(gameRepository.findCompletedGameIds(
                eq(BOARD_ID), eq(GameStatus.COMPLETED), isNull(), isNull(), isNull(), any(Pageable.class)))
                .willReturn(List.of(50L));
        given(gameRepository.findByIdInWithPlayers(List.of(50L))).willReturn(List.of(g50));

        GameCompletedGameResult result = gameCompletedGameQueryService
                .getCompletedGames(MEMBER_ID, BOARD_ID, null, null, 2);

        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
        assertThat(result.games()).hasSize(1);
    }

    @Test
    @DisplayName("courtNo 필터와 복합 커서(완료시각_id)가 리포지토리로 전달된다")
    void getCompletedGames_forwardsFilters() {
        LocalDateTime cursorTime = LocalDateTime.of(2026, 8, 17, 22, 30, 0);
        String cursor = cursorTime + "_100";

        given(gameBoardReader.read(BOARD_ID)).willReturn(board);
        given(gameRepository.findCompletedGameIds(
                eq(BOARD_ID), eq(GameStatus.COMPLETED), eq(3), eq(cursorTime), eq(100L), any(Pageable.class)))
                .willReturn(List.of());

        gameCompletedGameQueryService.getCompletedGames(MEMBER_ID, BOARD_ID, 3, cursor, 20);

        then(gameRepository).should().findCompletedGameIds(
                eq(BOARD_ID), eq(GameStatus.COMPLETED), eq(3), eq(cursorTime), eq(100L), any(Pageable.class));
    }

    @Test
    @DisplayName("완료 게임이 없으면 빈 IN 절 fetch를 건너뛰고 빈 결과를 반환한다")
    void getCompletedGames_emptyResultSkipsFetch() {
        given(gameBoardReader.read(BOARD_ID)).willReturn(board);
        given(gameRepository.findCompletedGameIds(
                eq(BOARD_ID), eq(GameStatus.COMPLETED), isNull(), isNull(), isNull(), any(Pageable.class)))
                .willReturn(List.of());

        GameCompletedGameResult result = gameCompletedGameQueryService
                .getCompletedGames(MEMBER_ID, BOARD_ID, null, null, 2);

        assertThat(result.games()).isEmpty();
        assertThat(result.nextCursor()).isNull();
        assertThat(result.hasNext()).isFalse();
        then(gameRepository).should(never()).findByIdInWithPlayers(anyCollection());
    }

    @Test
    @DisplayName("잘못된 커서는 INVALID_CURSOR(400)로 감싸고 조회를 시도하지 않는다")
    void getCompletedGames_invalidCursor() {
        given(gameBoardReader.read(BOARD_ID)).willReturn(board);

        // 구분자 없음 / 날짜 포맷 깨짐 / id 비숫자
        List<String> brokenCursors = List.of("no-separator", "not-a-date_100", "2026-08-17T22:30:00_abc");
        for (String cursor : brokenCursors) {
            assertThatThrownBy(() -> gameCompletedGameQueryService
                    .getCompletedGames(MEMBER_ID, BOARD_ID, null, cursor, 20))
                    .isInstanceOf(GameException.class)
                    .extracting(e -> ((GameException) e).getCode())
                    .isEqualTo(GameErrorCode.INVALID_CURSOR);
        }
        then(gameRepository).should(never()).findCompletedGameIds(
                any(), any(), any(), any(), any(), any(Pageable.class));
    }
}
