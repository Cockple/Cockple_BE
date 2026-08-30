package umc.cockple.demo.domain.game.service.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.service.GamePairCount;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.service.query.result.GameDuplicateCheckResult;
import umc.cockple.demo.domain.game.service.query.result.GameDuplicateCheckResult.PairView;
import umc.cockple.demo.domain.game.domain.service.GamePairHistoryCalculator;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardMemberReader;
import umc.cockple.demo.domain.game.service.support.reader.GameReader;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.GameFixture;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameDuplicateCheckQueryService")
class GameDuplicateCheckQueryServiceTest {

    @Mock private GameBoardReader gameBoardReader;
    @Mock private GameReader gameReader;
    @Mock private GameBoardMemberReader gameBoardMemberReader;

    private GameDuplicateCheckQueryService gameDuplicateCheckQueryService;

    private static final Long MEMBER_ID = 100L;
    private static final Long BOARD_ID = 1L;
    private GameBoard board;

    @BeforeEach
    void setUp() {
        board = GameFixture.gameBoard(BOARD_ID);
        gameDuplicateCheckQueryService = new GameDuplicateCheckQueryService(
                gameBoardReader,
                gameReader,
                gameBoardMemberReader,
                new GamePairHistoryCalculator());
    }

    @Test
    @DisplayName("선택 멤버의 쌍별로 함께 완료한 게임 수와 직전 게임 동반 여부를 계산한다")
    void checkDuplicates_computesPairCountsAndLastGame() {
        // given - 멤버 7,8,9,10
        GameBoardMember m7 = GameFixture.member(7L, board, "7", Level.A);
        GameBoardMember m8 = GameFixture.member(8L, board, "8", Level.A);
        GameBoardMember m9 = GameFixture.member(9L, board, "9", Level.A);
        GameBoardMember m10 = GameFixture.member(10L, board, "10", Level.A);
        List<Long> selected = List.of(7L, 8L, 9L, 10L);
        given(gameBoardReader.read(BOARD_ID)).willReturn(board);
        given(gameBoardMemberReader.readAllByGameBoardAndIds(BOARD_ID, selected))
                .willReturn(List.of(m7, m8, m9, m10));
        given(gameReader.readCompletedPairCounts(BOARD_ID, selected)).willReturn(List.of(
                new GamePairCount(7L, 8L, 2),
                new GamePairCount(7L, 9L, 1),
                new GamePairCount(7L, 10L, 1),
                new GamePairCount(8L, 9L, 1),
                new GamePairCount(8L, 10L, 1),
                new GamePairCount(9L, 10L, 1)));
        given(gameReader.readLatestCompletedGameMemberIds(BOARD_ID))
                .willReturn(List.of(7L, 8L, 11L, 12L));

        // when
        GameDuplicateCheckResult result = gameDuplicateCheckQueryService.checkDuplicates(MEMBER_ID, BOARD_ID, selected);

        // then - 4명이면 6쌍
        assertThat(result.pairs()).hasSize(6);
        // (7,8): 두 게임 모두 함께 → count 2, 직전 게임에도 함께 → true
        PairView pair78 = pairOf(result, 7L, 8L);
        assertThat(pair78.count()).isEqualTo(2);
        assertThat(pair78.playedInLastGame()).isTrue();
        // (7,9): 게임1에만 함께 → count 1, 직전 게임엔 9 없음 → false
        PairView pair79 = pairOf(result, 7L, 9L);
        assertThat(pair79.count()).isEqualTo(1);
        assertThat(pair79.playedInLastGame()).isFalse();
        // (9,10): 게임1에만 함께 → count 1, false
        PairView pair910 = pairOf(result, 9L, 10L);
        assertThat(pair910.count()).isEqualTo(1);
        assertThat(pair910.playedInLastGame()).isFalse();
    }

    @Test
    @DisplayName("완료된 게임이 없으면 모든 쌍의 count는 0, playedInLastGame은 false")
    void checkDuplicates_noCompletedGames() {
        GameBoardMember m7 = GameFixture.member(7L, board, "7", Level.A);
        GameBoardMember m8 = GameFixture.member(8L, board, "8", Level.A);
        List<Long> selected = List.of(7L, 8L);
        given(gameBoardReader.read(BOARD_ID)).willReturn(board);
        given(gameBoardMemberReader.readAllByGameBoardAndIds(BOARD_ID, selected))
                .willReturn(List.of(m7, m8));
        given(gameReader.readCompletedPairCounts(BOARD_ID, selected)).willReturn(List.of());
        given(gameReader.readLatestCompletedGameMemberIds(BOARD_ID)).willReturn(List.of());

        GameDuplicateCheckResult result = gameDuplicateCheckQueryService.checkDuplicates(MEMBER_ID, BOARD_ID, selected);

        assertThat(result.pairs()).hasSize(1);
        assertThat(result.pairs().get(0).count()).isZero();
        assertThat(result.pairs().get(0).playedInLastGame()).isFalse();
    }

    @Test
    @DisplayName("선택 멤버가 게임판 명단에 없으면 GAME_BOARD_MEMBER_NOT_FOUND 예외")
    void checkDuplicates_memberNotOnBoard() {
        GameBoardMember m7 = GameFixture.member(7L, board, "7", Level.A);
        List<Long> selected = List.of(7L, 999L);
        given(gameBoardReader.read(BOARD_ID)).willReturn(board);
        given(gameBoardMemberReader.readAllByGameBoardAndIds(BOARD_ID, selected))
                .willReturn(List.of(m7));

        assertThatThrownBy(() -> gameDuplicateCheckQueryService.checkDuplicates(MEMBER_ID, BOARD_ID, selected))
                .isInstanceOf(GameException.class)
                .extracting(e -> ((GameException) e).getCode())
                .isEqualTo(GameErrorCode.GAME_BOARD_MEMBER_NOT_FOUND);
    }

    private PairView pairOf(GameDuplicateCheckResult result, Long a, Long b) {
        return result.pairs().stream()
                .filter(pair -> pair.memberIdA().equals(a) && pair.memberIdB().equals(b))
                .findFirst()
                .orElseThrow();
    }
}
