package umc.cockple.demo.support.fixture;

import umc.cockple.demo.domain.game.domain.Court;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.domain.GamePlayer;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 게임판 도메인 테스트용 픽스처. 엔티티 @Builder 로 id 까지 세팅해 유닛 테스트에서 사용한다.
 */
public class GameFixture {

    public static GameBoard gameBoard(Long id) {
        return GameBoard.builder().id(id).build();
    }

    public static Court court(Long id, GameBoard gameBoard, int courtNo, String courtName) {
        return Court.builder()
                .id(id)
                .gameBoard(gameBoard)
                .courtNo(courtNo)
                .courtName(courtName)
                .build();
    }

    public static GameBoardMember member(Long id, GameBoard gameBoard, String name, Level level) {
        return GameBoardMember.builder()
                .id(id)
                .gameBoard(gameBoard)
                .name(name)
                .gender(Gender.MALE)
                .level(level)
                .shuttlecockSubmitted(false)
                .participating(true)
                .gameCount(0)
                .build();
    }

    public static GamePlayer player(GameBoardMember member, int playerOrder) {
        return GamePlayer.builder()
                .gameBoardMember(member)
                .playerOrder(playerOrder)
                .build();
    }

    public static Game playingGame(Long id, GameBoard gameBoard, Court court, LocalDateTime startedAt, GamePlayer... players) {
        return Game.builder()
                .id(id)
                .gameBoard(gameBoard)
                .court(court)
                .status(GameStatus.PLAYING)
                .startedAt(startedAt)
                .players(new ArrayList<>(List.of(players)))
                .build();
    }

    public static Game waitingGame(Long id, GameBoard gameBoard, int waitingOrder, GamePlayer... players) {
        return Game.builder()
                .id(id)
                .gameBoard(gameBoard)
                .status(GameStatus.WAITING)
                .waitingOrder(waitingOrder)
                .players(new ArrayList<>(List.of(players)))
                .build();
    }

    public static Game completedGame(Long id, GameBoard gameBoard, LocalDateTime completedAt, GamePlayer... players) {
        return Game.builder()
                .id(id)
                .gameBoard(gameBoard)
                .status(GameStatus.COMPLETED)
                .completedAt(completedAt)
                .players(new ArrayList<>(List.of(players)))
                .build();
    }
}
