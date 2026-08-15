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
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.repository.CourtRepository;
import umc.cockple.demo.domain.game.repository.GameRepository;
import umc.cockple.demo.domain.game.service.command.model.GameCourtManageCommand;
import umc.cockple.demo.domain.game.service.command.model.GameCourtManageCommand.CourtCommand;
import umc.cockple.demo.domain.game.service.command.model.GameCourtMoveCommand;
import umc.cockple.demo.domain.game.service.command.result.GameCourtManageResult;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;
import umc.cockple.demo.support.fixture.GameFixture;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameCourtCommandService")
class GameCourtCommandServiceTest {

    @Mock private GameBoardReader gameBoardReader;
    @Mock private CourtRepository courtRepository;
    @Mock private GameRepository gameRepository;

    @InjectMocks private GameCourtCommandService gameCourtCommandService;

    private static final Long MEMBER_ID = 100L;
    private static final Long BOARD_ID = 1L;
    private GameBoard board;

    @BeforeEach
    void setUp() {
        board = GameFixture.gameBoard(BOARD_ID);
    }

    @Nested
    @DisplayName("manageCourts (#1 코트 관리)")
    class ManageCourts {

        @Test
        @DisplayName("유지(이름변경)/신규생성/삭제를 한 번에 반영하고 courtNo를 배열 순서로 재부여한다")
        void manageCourts_upsertAndDeleteInOrder() {
            // given
            Court kept = GameFixture.court(10L, board, 1, "기존코트");
            Court removed = GameFixture.court(11L, board, 2, "삭제될코트");
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(courtRepository.findByGameBoardId(BOARD_ID)).willReturn(List.of(kept, removed));
            given(courtRepository.save(any(Court.class))).willAnswer(inv -> inv.getArgument(0));

            GameCourtManageCommand command = new GameCourtManageCommand(BOARD_ID, List.of(
                    new CourtCommand(10L, "새이름"), // 유지 + 이름변경 → courtNo 1
                    new CourtCommand(null, "신규코트") // 신규 → courtNo 2
            )); // 기존 removed(11L)는 목록에 없으므로 삭제

            // when
            GameCourtManageResult result = gameCourtCommandService.manageCourts(MEMBER_ID, command);

            // then - 유지 코트는 이름/순서 갱신
            assertThat(kept.getCourtNo()).isEqualTo(1);
            assertThat(kept.getCourtName()).isEqualTo("새이름");

            // 빠진 코트는 삭제
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Court>> deleteCaptor = ArgumentCaptor.forClass(List.class);
            then(courtRepository).should().deleteAll(deleteCaptor.capture());
            assertThat(deleteCaptor.getValue()).containsExactly(removed);

            // 신규 코트는 courtNo 2로 저장
            ArgumentCaptor<Court> saveCaptor = ArgumentCaptor.forClass(Court.class);
            then(courtRepository).should().save(saveCaptor.capture());
            assertThat(saveCaptor.getValue().getCourtNo()).isEqualTo(2);
            assertThat(saveCaptor.getValue().getCourtName()).isEqualTo("신규코트");

            // 결과는 최종 코트 2개
            assertThat(result.gameBoardId()).isEqualTo(BOARD_ID);
            assertThat(result.courts()).hasSize(2);
        }

        @Test
        @DisplayName("courtName이 비어 있으면 'N번 코트' 기본값을 부여한다")
        void manageCourts_defaultCourtName() {
            // given
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(courtRepository.findByGameBoardId(BOARD_ID)).willReturn(List.of());
            given(courtRepository.save(any(Court.class))).willAnswer(inv -> inv.getArgument(0));

            GameCourtManageCommand command = new GameCourtManageCommand(BOARD_ID, List.of(
                    new CourtCommand(null, null),
                    new CourtCommand(null, "   ") // 공백도 기본값 처리
            ));

            // when
            gameCourtCommandService.manageCourts(MEMBER_ID, command);

            // then
            ArgumentCaptor<Court> saveCaptor = ArgumentCaptor.forClass(Court.class);
            then(courtRepository).should(times(2)).save(saveCaptor.capture());
            assertThat(saveCaptor.getAllValues())
                    .extracting(Court::getCourtName)
                    .containsExactly("1번 코트", "2번 코트");
        }

        @Test
        @DisplayName("gameBoardId가 null이면 GAME_BOARD_ID_REQUIRED 예외")
        void manageCourts_nullBoardId() {
            GameCourtManageCommand command = new GameCourtManageCommand(null, List.of());

            assertThatThrownBy(() -> gameCourtCommandService.manageCourts(MEMBER_ID, command))
                    .isInstanceOf(GameException.class)
                    .extracting(e -> ((GameException) e).getCode())
                    .isEqualTo(GameErrorCode.GAME_BOARD_ID_REQUIRED);
        }

        @Test
        @DisplayName("요청 courtId가 해당 게임판에 없으면 COURT_NOT_FOUND 예외")
        void manageCourts_foreignCourtId() {
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(courtRepository.findByGameBoardId(BOARD_ID))
                    .willReturn(List.of(GameFixture.court(10L, board, 1, "코트")));

            GameCourtManageCommand command = new GameCourtManageCommand(BOARD_ID, List.of(
                    new CourtCommand(999L, "없는코트")
            ));

            assertThatThrownBy(() -> gameCourtCommandService.manageCourts(MEMBER_ID, command))
                    .isInstanceOf(GameException.class)
                    .extracting(e -> ((GameException) e).getCode())
                    .isEqualTo(GameErrorCode.COURT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("moveCourt (#3 코트 위치 변경)")
    class MoveCourt {

        @Test
        @DisplayName("목적지 코트가 비어 있으면 게임을 그 코트로 이동한다")
        void moveCourt_toEmptyCourt() {
            // given
            Court source = GameFixture.court(10L, board, 1, "1번");
            Court target = GameFixture.court(11L, board, 2, "2번");
            Game sourceGame = GameFixture.playingGame(50L, board, source, LocalDateTime.now());

            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findByCourtIdAndStatus(10L, GameStatus.PLAYING)).willReturn(Optional.of(sourceGame));
            given(courtRepository.findByGameBoardIdAndCourtNo(BOARD_ID, 2)).willReturn(Optional.of(target));
            given(gameRepository.findByCourtIdAndStatus(11L, GameStatus.PLAYING)).willReturn(Optional.empty());

            // when
            gameCourtCommandService.moveCourt(MEMBER_ID, new GameCourtMoveCommand(BOARD_ID, 10L, 2));

            // then
            assertThat(sourceGame.getCourt()).isEqualTo(target);
        }

        @Test
        @DisplayName("목적지 코트에 게임이 있으면 두 게임의 코트를 맞바꾼다(swap)")
        void moveCourt_swap() {
            // given
            Court source = GameFixture.court(10L, board, 1, "1번");
            Court target = GameFixture.court(11L, board, 2, "2번");
            Game sourceGame = GameFixture.playingGame(50L, board, source, LocalDateTime.now());
            Game targetGame = GameFixture.playingGame(51L, board, target, LocalDateTime.now());

            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findByCourtIdAndStatus(10L, GameStatus.PLAYING)).willReturn(Optional.of(sourceGame));
            given(courtRepository.findByGameBoardIdAndCourtNo(BOARD_ID, 2)).willReturn(Optional.of(target));
            given(gameRepository.findByCourtIdAndStatus(11L, GameStatus.PLAYING)).willReturn(Optional.of(targetGame));

            // when
            gameCourtCommandService.moveCourt(MEMBER_ID, new GameCourtMoveCommand(BOARD_ID, 10L, 2));

            // then
            assertThat(sourceGame.getCourt()).isEqualTo(target);
            assertThat(targetGame.getCourt()).isEqualTo(source);
        }

        @Test
        @DisplayName("원본 코트에 진행 중인 게임이 없으면 GAME_NOT_ON_COURT 예외")
        void moveCourt_noGameOnSource() {
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findByCourtIdAndStatus(10L, GameStatus.PLAYING)).willReturn(Optional.empty());

            assertThatThrownBy(() -> gameCourtCommandService.moveCourt(MEMBER_ID, new GameCourtMoveCommand(BOARD_ID, 10L, 2)))
                    .isInstanceOf(GameException.class)
                    .extracting(e -> ((GameException) e).getCode())
                    .isEqualTo(GameErrorCode.GAME_NOT_ON_COURT);
        }

        @Test
        @DisplayName("목적지 코트 번호가 존재하지 않으면 COURT_NOT_FOUND 예외")
        void moveCourt_targetCourtNotFound() {
            Court source = GameFixture.court(10L, board, 1, "1번");
            Game sourceGame = GameFixture.playingGame(50L, board, source, LocalDateTime.now());
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findByCourtIdAndStatus(10L, GameStatus.PLAYING)).willReturn(Optional.of(sourceGame));
            given(courtRepository.findByGameBoardIdAndCourtNo(BOARD_ID, 9)).willReturn(Optional.empty());

            assertThatThrownBy(() -> gameCourtCommandService.moveCourt(MEMBER_ID, new GameCourtMoveCommand(BOARD_ID, 10L, 9)))
                    .isInstanceOf(GameException.class)
                    .extracting(e -> ((GameException) e).getCode())
                    .isEqualTo(GameErrorCode.COURT_NOT_FOUND);
        }

        @Test
        @DisplayName("같은 코트로의 이동은 아무 것도 바꾸지 않는다")
        void moveCourt_sameCourtNoop() {
            Court source = GameFixture.court(10L, board, 1, "1번");
            Game sourceGame = GameFixture.playingGame(50L, board, source, LocalDateTime.now());
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findByCourtIdAndStatus(10L, GameStatus.PLAYING)).willReturn(Optional.of(sourceGame));
            given(courtRepository.findByGameBoardIdAndCourtNo(BOARD_ID, 1)).willReturn(Optional.of(source));

            gameCourtCommandService.moveCourt(MEMBER_ID, new GameCourtMoveCommand(BOARD_ID, 10L, 1));

            // 코트가 그대로 유지된다(조기 반환)
            assertThat(sourceGame.getCourt()).isEqualTo(source);
        }
    }
}
