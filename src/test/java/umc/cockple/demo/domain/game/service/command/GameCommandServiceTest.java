package umc.cockple.demo.domain.game.service.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.game.domain.Court;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.domain.GamePlayer;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.repository.CourtRepository;
import umc.cockple.demo.domain.game.repository.GameBoardMemberRepository;
import umc.cockple.demo.domain.game.repository.GameRepository;
import umc.cockple.demo.domain.game.service.command.model.GameCompleteCommand;
import umc.cockple.demo.domain.game.service.command.model.GameCreateCommand;
import umc.cockple.demo.domain.game.service.command.model.GameStartCommand;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.GameFixture;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameCommandService")
class GameCommandServiceTest {

    @Mock private GameBoardReader gameBoardReader;
    @Mock private GameRepository gameRepository;
    @Mock private CourtRepository courtRepository;
    @Mock private GameBoardMemberRepository gameBoardMemberRepository;

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

    @Nested
    @DisplayName("createGame (#8 게임 대기 생성)")
    class CreateGame {

        @Test
        @DisplayName("선택 멤버로 대기 게임을 만들고, 배열 순서를 playerOrder로 대기열 맨 뒤에 붙인다")
        void createGame_createsWaitingGameWithOrderedPlayers() {
            // given - 이미 대기 게임이 2개 있으므로 새 게임의 waitingOrder는 3
            GameBoardMember m7 = GameFixture.member(7L, board, "선수7", Level.A);
            GameBoardMember m8 = GameFixture.member(8L, board, "선수8", Level.B);
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameBoardMemberRepository.findByGameBoardIdAndIdIn(BOARD_ID, List.of(8L, 7L)))
                    .willReturn(List.of(m7, m8));
            given(gameRepository.countByGameBoardIdAndStatus(BOARD_ID, GameStatus.WAITING)).willReturn(2L);
            given(gameRepository.save(any(Game.class))).willAnswer(inv -> inv.getArgument(0));

            // when - 입력 순서 8, 7
            gameCommandService.createGame(MEMBER_ID, new GameCreateCommand(BOARD_ID, List.of(8L, 7L)));

            // then
            ArgumentCaptor<Game> captor = ArgumentCaptor.forClass(Game.class);
            then(gameRepository).should().save(captor.capture());
            Game saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(GameStatus.WAITING);
            assertThat(saved.getWaitingOrder()).isEqualTo(3);
            assertThat(saved.getPlayers())
                    .extracting(p -> p.getGameBoardMember().getId(), GamePlayer::getPlayerOrder)
                    .containsExactly(tuple(8L, 0), tuple(7L, 1));
        }

        @Test
        @DisplayName("명단에 없는 멤버가 포함되면 GAME_BOARD_MEMBER_NOT_FOUND 예외")
        void createGame_memberNotOnBoard() {
            GameBoardMember m7 = GameFixture.member(7L, board, "선수7", Level.A);
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameBoardMemberRepository.findByGameBoardIdAndIdIn(BOARD_ID, List.of(7L, 999L)))
                    .willReturn(List.of(m7)); // 999는 조회되지 않음

            assertThatThrownBy(() -> gameCommandService.createGame(MEMBER_ID, new GameCreateCommand(BOARD_ID, List.of(7L, 999L))))
                    .isInstanceOf(GameException.class)
                    .extracting(e -> ((GameException) e).getCode())
                    .isEqualTo(GameErrorCode.GAME_BOARD_MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("인원이 없거나 4명을 초과하면 INVALID_GAME_PLAYER_COUNT 예외 (command 검증)")
        void createGame_invalidPlayerCount() {
            assertThatThrownBy(() -> new GameCreateCommand(BOARD_ID, List.of()))
                    .isInstanceOf(GameException.class)
                    .extracting(e -> ((GameException) e).getCode())
                    .isEqualTo(GameErrorCode.INVALID_GAME_PLAYER_COUNT);

            assertThatThrownBy(() -> new GameCreateCommand(BOARD_ID, List.of(1L, 2L, 3L, 4L, 5L)))
                    .isInstanceOf(GameException.class)
                    .extracting(e -> ((GameException) e).getCode())
                    .isEqualTo(GameErrorCode.INVALID_GAME_PLAYER_COUNT);
        }

        @Test
        @DisplayName("같은 멤버가 중복되면 DUPLICATE_GAME_PLAYER 예외 (command 검증)")
        void createGame_duplicatePlayer() {
            assertThatThrownBy(() -> new GameCreateCommand(BOARD_ID, List.of(7L, 7L)))
                    .isInstanceOf(GameException.class)
                    .extracting(e -> ((GameException) e).getCode())
                    .isEqualTo(GameErrorCode.DUPLICATE_GAME_PLAYER);
        }

        @Test
        @DisplayName("gameBoardId가 null이면 GAME_BOARD_ID_REQUIRED 예외 (command 검증)")
        void createGame_nullBoardId() {
            assertThatThrownBy(() -> new GameCreateCommand(null, List.of(7L)))
                    .isInstanceOf(GameException.class)
                    .extracting(e -> ((GameException) e).getCode())
                    .isEqualTo(GameErrorCode.GAME_BOARD_ID_REQUIRED);
        }
    }

    @Nested
    @DisplayName("completeGame (#5 게임 완료)")
    class CompleteGame {

        @Test
        @DisplayName("진행 게임을 완료해 코트를 비우고, 참여 멤버의 게임횟수를 1 증가시킨다")
        void completeGame_completesAndIncrementsGameCount() {
            // given
            GameBoardMember m1 = GameFixture.member(7L, board, "선수A", Level.A);
            GameBoardMember m2 = GameFixture.member(8L, board, "선수B", Level.B);
            Game playing = GameFixture.playingGame(GAME_ID, board, court, LocalDateTime.now(),
                    GameFixture.player(m1, 0), GameFixture.player(m2, 1));
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findById(GAME_ID)).willReturn(Optional.of(playing));

            // when
            Long completedGameId = gameCommandService.completeGame(MEMBER_ID, new GameCompleteCommand(BOARD_ID, GAME_ID));

            // then
            assertThat(completedGameId).isEqualTo(GAME_ID);
            assertThat(playing.getStatus()).isEqualTo(GameStatus.COMPLETED);
            assertThat(playing.getCourt()).isNull();
            assertThat(playing.getCompletedAt()).isNotNull();
            assertThat(m1.getGameCount()).isEqualTo(1);
            assertThat(m2.getGameCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("게임을 찾을 수 없으면 GAME_NOT_FOUND 예외")
        void completeGame_gameNotFound() {
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findById(GAME_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> gameCommandService.completeGame(MEMBER_ID, new GameCompleteCommand(BOARD_ID, GAME_ID)))
                    .isInstanceOf(GameException.class)
                    .extracting(e -> ((GameException) e).getCode())
                    .isEqualTo(GameErrorCode.GAME_NOT_FOUND);
        }

        @Test
        @DisplayName("다른 게임판의 게임이면 GAME_NOT_FOUND 예외")
        void completeGame_gameOfOtherBoard() {
            GameBoard otherBoard = GameFixture.gameBoard(2L);
            Game gameOfOtherBoard = GameFixture.playingGame(GAME_ID, otherBoard, court, LocalDateTime.now());
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findById(GAME_ID)).willReturn(Optional.of(gameOfOtherBoard));

            assertThatThrownBy(() -> gameCommandService.completeGame(MEMBER_ID, new GameCompleteCommand(BOARD_ID, GAME_ID)))
                    .isInstanceOf(GameException.class)
                    .extracting(e -> ((GameException) e).getCode())
                    .isEqualTo(GameErrorCode.GAME_NOT_FOUND);
        }

        @Test
        @DisplayName("진행 중인 게임이 아니면 GAME_NOT_PLAYING 예외")
        void completeGame_notPlaying() {
            Game waiting = GameFixture.waitingGame(GAME_ID, board, 1);
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findById(GAME_ID)).willReturn(Optional.of(waiting));

            assertThatThrownBy(() -> gameCommandService.completeGame(MEMBER_ID, new GameCompleteCommand(BOARD_ID, GAME_ID)))
                    .isInstanceOf(GameException.class)
                    .extracting(e -> ((GameException) e).getCode())
                    .isEqualTo(GameErrorCode.GAME_NOT_PLAYING);
        }
    }
}
