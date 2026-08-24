package umc.cockple.demo.domain.game.service.support.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.service.support.calculator.GamePairHistoryCalculator.GamePairHistory;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.GameFixture;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GamePairHistoryCalculator")
class GamePairHistoryCalculatorTest {

    private final GamePairHistoryCalculator calculator = new GamePairHistoryCalculator();

    @Test
    @DisplayName("완료 게임의 모든 쌍별 횟수와 직전 경기 포함 여부를 계산한다")
    void calculate_countsCompletedGamesAndLastGamePairs() {
        GameBoard gameBoard = GameFixture.gameBoard(1L);
        GameBoardMember first = GameFixture.member(1L, gameBoard, "첫 번째", Level.A);
        GameBoardMember second = GameFixture.member(2L, gameBoard, "두 번째", Level.A);
        GameBoardMember third = GameFixture.member(3L, gameBoard, "세 번째", Level.A);
        GameBoardMember fourth = GameFixture.member(4L, gameBoard, "네 번째", Level.A);
        Game earlier = GameFixture.completedGame(
                1L,
                gameBoard,
                LocalDateTime.of(2026, 8, 21, 10, 0),
                GameFixture.player(first, 0),
                GameFixture.player(second, 1),
                GameFixture.player(third, 2));
        Game last = GameFixture.completedGame(
                2L,
                gameBoard,
                LocalDateTime.of(2026, 8, 21, 11, 0),
                GameFixture.player(first, 0),
                GameFixture.player(second, 1),
                GameFixture.player(fourth, 2));

        GamePairHistory history = calculator.calculate(List.of(earlier, last));

        assertThat(history.count(first.getId(), second.getId())).isEqualTo(2);
        assertThat(history.count(first.getId(), third.getId())).isEqualTo(1);
        assertThat(history.count(third.getId(), fourth.getId())).isZero();
        assertThat(history.playedInLastGame(first.getId(), second.getId())).isTrue();
        assertThat(history.playedInLastGame(first.getId(), third.getId())).isFalse();
    }

    @Test
    @DisplayName("멤버 ID 순서와 관계없이 같은 쌍의 이력을 반환한다")
    void calculate_normalizesMemberIdOrder() {
        GameBoard gameBoard = GameFixture.gameBoard(1L);
        GameBoardMember first = GameFixture.member(1L, gameBoard, "첫 번째", Level.A);
        GameBoardMember second = GameFixture.member(2L, gameBoard, "두 번째", Level.A);
        Game completed = GameFixture.completedGame(
                1L,
                gameBoard,
                LocalDateTime.of(2026, 8, 21, 10, 0),
                GameFixture.player(first, 0),
                GameFixture.player(second, 1));

        GamePairHistory history = calculator.calculate(List.of(completed));

        assertThat(history.count(second.getId(), first.getId())).isEqualTo(1);
        assertThat(history.playedInLastGame(second.getId(), first.getId())).isTrue();
    }

    @Test
    @DisplayName("완료 게임이 없으면 쌍 이력은 비어 있다")
    void calculate_returnsEmptyHistoryWithoutCompletedGames() {
        GamePairHistory history = calculator.calculate(List.of());

        assertThat(history.count(1L, 2L)).isZero();
        assertThat(history.playedInLastGame(1L, 2L)).isFalse();
    }
}
