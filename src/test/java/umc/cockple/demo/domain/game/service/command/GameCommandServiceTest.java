package umc.cockple.demo.domain.game.service.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.game.domain.Court;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.repository.CourtRepository;
import umc.cockple.demo.domain.game.repository.GameRepository;
import umc.cockple.demo.domain.game.service.command.model.GameStartCommand;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;
import umc.cockple.demo.support.fixture.GameFixture;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameCommandService")
class GameCommandServiceTest {

    @Mock private GameBoardReader gameBoardReader;
    @Mock private GameRepository gameRepository;
    @Mock private CourtRepository courtRepository;

    @InjectMocks private GameCommandService gameCommandService;

    private static final Long MEMBER_ID = 100L;
    private static final Long BOARD_ID = 1L;
    private static final Long GAME_ID = 50L;
    private static final Long COURT_ID = 10L;

    private GameBoard board;
    private Court court;

    @BeforeEach
    void setUp() {
        board = GameFixture.gameBoard(BOARD_ID);
        court = GameFixture.court(COURT_ID, board, 1, "1번 코트");
    }

    @Nested
    @DisplayName("startGame (#4 게임 시작)")
    class StartGame {

        @Test
        @DisplayName("대기 게임을 코트에 배치해 진행 상태로 만든다")
        void startGame_placesWaitingGameOnCourt() {
            // given
            Game waiting = GameFixture.waitingGame(GAME_ID, board, 1);
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findById(GAME_ID)).willReturn(Optional.of(waiting));
            given(courtRepository.findByIdAndGameBoardId(COURT_ID, BOARD_ID)).willReturn(Optional.of(court));
            given(gameRepository.findByCourtIdAndStatus(COURT_ID, GameStatus.PLAYING)).willReturn(Optional.empty());
            given(gameRepository.findByGameBoardIdAndStatusOrderByWaitingOrderAsc(BOARD_ID, GameStatus.WAITING))
                    .willReturn(List.of());

            // when
            gameCommandService.startGame(MEMBER_ID, new GameStartCommand(BOARD_ID, GAME_ID, COURT_ID));

            // then
            assertThat(waiting.getStatus()).isEqualTo(GameStatus.PLAYING);
            assertThat(waiting.getCourt()).isEqualTo(court);
            assertThat(waiting.getStartedAt()).isNotNull();
            assertThat(waiting.getWaitingOrder()).isNull();
        }

        @Test
        @DisplayName("시작 후 남은 대기열의 순서를 1부터 다시 매긴다")
        void startGame_resequencesWaitingQueue() {
            // given
            Game waiting = GameFixture.waitingGame(GAME_ID, board, 1);
            Game remaining1 = GameFixture.waitingGame(51L, board, 2);
            Game remaining2 = GameFixture.waitingGame(52L, board, 3);
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findById(GAME_ID)).willReturn(Optional.of(waiting));
            given(courtRepository.findByIdAndGameBoardId(COURT_ID, BOARD_ID)).willReturn(Optional.of(court));
            given(gameRepository.findByCourtIdAndStatus(COURT_ID, GameStatus.PLAYING)).willReturn(Optional.empty());
            given(gameRepository.findByGameBoardIdAndStatusOrderByWaitingOrderAsc(BOARD_ID, GameStatus.WAITING))
                    .willReturn(List.of(remaining1, remaining2));

            // when
            gameCommandService.startGame(MEMBER_ID, new GameStartCommand(BOARD_ID, GAME_ID, COURT_ID));

            // then
            assertThat(remaining1.getWaitingOrder()).isEqualTo(1);
            assertThat(remaining2.getWaitingOrder()).isEqualTo(2);
        }

        @Test
        @DisplayName("게임을 찾을 수 없으면 GAME_NOT_FOUND 예외")
        void startGame_gameNotFound() {
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findById(GAME_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> gameCommandService.startGame(MEMBER_ID, new GameStartCommand(BOARD_ID, GAME_ID, COURT_ID)))
                    .isInstanceOf(GameException.class)
                    .extracting(e -> ((GameException) e).getCode())
                    .isEqualTo(GameErrorCode.GAME_NOT_FOUND);
        }

        @Test
        @DisplayName("다른 게임판의 게임이면 GAME_NOT_FOUND 예외")
        void startGame_gameOfOtherBoard() {
            GameBoard otherBoard = GameFixture.gameBoard(2L);
            Game gameOfOtherBoard = GameFixture.waitingGame(GAME_ID, otherBoard, 1);
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findById(GAME_ID)).willReturn(Optional.of(gameOfOtherBoard));

            assertThatThrownBy(() -> gameCommandService.startGame(MEMBER_ID, new GameStartCommand(BOARD_ID, GAME_ID, COURT_ID)))
                    .isInstanceOf(GameException.class)
                    .extracting(e -> ((GameException) e).getCode())
                    .isEqualTo(GameErrorCode.GAME_NOT_FOUND);
        }

        @Test
        @DisplayName("대기 상태가 아닌 게임이면 GAME_NOT_WAITING 예외")
        void startGame_notWaiting() {
            Game playing = GameFixture.playingGame(GAME_ID, board, court, LocalDateTime.now());
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findById(GAME_ID)).willReturn(Optional.of(playing));

            assertThatThrownBy(() -> gameCommandService.startGame(MEMBER_ID, new GameStartCommand(BOARD_ID, GAME_ID, COURT_ID)))
                    .isInstanceOf(GameException.class)
                    .extracting(e -> ((GameException) e).getCode())
                    .isEqualTo(GameErrorCode.GAME_NOT_WAITING);
        }

        @Test
        @DisplayName("목적지 코트가 없으면 COURT_NOT_FOUND 예외")
        void startGame_courtNotFound() {
            Game waiting = GameFixture.waitingGame(GAME_ID, board, 1);
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findById(GAME_ID)).willReturn(Optional.of(waiting));
            given(courtRepository.findByIdAndGameBoardId(COURT_ID, BOARD_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> gameCommandService.startGame(MEMBER_ID, new GameStartCommand(BOARD_ID, GAME_ID, COURT_ID)))
                    .isInstanceOf(GameException.class)
                    .extracting(e -> ((GameException) e).getCode())
                    .isEqualTo(GameErrorCode.COURT_NOT_FOUND);
        }

        @Test
        @DisplayName("목적지 코트에 이미 진행 중인 게임이 있으면 COURT_ALREADY_IN_USE 예외")
        void startGame_courtAlreadyInUse() {
            Game waiting = GameFixture.waitingGame(GAME_ID, board, 1);
            Game occupying = GameFixture.playingGame(99L, board, court, LocalDateTime.now());
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findById(GAME_ID)).willReturn(Optional.of(waiting));
            given(courtRepository.findByIdAndGameBoardId(COURT_ID, BOARD_ID)).willReturn(Optional.of(court));
            given(gameRepository.findByCourtIdAndStatus(COURT_ID, GameStatus.PLAYING)).willReturn(Optional.of(occupying));

            assertThatThrownBy(() -> gameCommandService.startGame(MEMBER_ID, new GameStartCommand(BOARD_ID, GAME_ID, COURT_ID)))
                    .isInstanceOf(GameException.class)
                    .extracting(e -> ((GameException) e).getCode())
                    .isEqualTo(GameErrorCode.COURT_ALREADY_IN_USE);
        }
    }
}
