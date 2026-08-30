package umc.cockple.demo.domain.game.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DisplayName("GameBoard")
class GameBoardTest {

    @Test
    @DisplayName("게임판 생성 시 기본 코트 2개(1번·2번 코트)를 부여한다")
    void create_seedsTwoDefaultCourts() {
        GameBoard gameBoard = GameBoard.create();

        assertThat(gameBoard.getCourts())
                .extracting(Court::getCourtNo, Court::getCourtName)
                .containsExactly(tuple(1, "1번 코트"), tuple(2, "2번 코트"));
    }

    @Test
    @DisplayName("기본 코트는 생성된 게임판에 연결되어 있다")
    void create_defaultCourtsBelongToBoard() {
        GameBoard gameBoard = GameBoard.create();

        assertThat(gameBoard.getCourts())
                .allSatisfy(court -> assertThat(court.getGameBoard()).isSameAs(gameBoard));
    }
}
