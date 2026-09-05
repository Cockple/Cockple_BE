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
import org.springframework.context.ApplicationEventPublisher;
import umc.cockple.demo.domain.game.domain.Court;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.domain.GamePlayer;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.game.domain.service.GameBoardMemberAvailabilityPolicy;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.events.GameBoardMembersChangedEvent;
import umc.cockple.demo.domain.game.events.GameStartedEvent;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.repository.CourtRepository;
import umc.cockple.demo.domain.game.repository.GameBoardMemberRepository;
import umc.cockple.demo.domain.game.repository.GameRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.game.service.command.model.GameCompleteCommand;
import umc.cockple.demo.domain.game.service.command.model.GameCreateCommand;
import umc.cockple.demo.domain.game.service.command.model.GameDeleteCommand;
import umc.cockple.demo.domain.game.service.command.model.GameStartCommand;
import umc.cockple.demo.domain.game.service.command.model.GameToWaitingCommand;
import umc.cockple.demo.domain.game.service.command.result.GameDeleteResult;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;
import umc.cockple.demo.domain.game.service.support.validator.GameBoardAccessValidator;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.GameFixture;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameCommandService")
class GameCommandServiceTest {

    @Mock private GameBoardReader gameBoardReader;
    @Mock private GameRepository gameRepository;
    @Mock private CourtRepository courtRepository;
    @Mock private GameBoardMemberRepository gameBoardMemberRepository;
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private GameBoardAccessValidator gameBoardAccessValidator;
    @Mock private GameBoardMemberAvailabilityPolicy availabilityPolicy;
    @Mock private ApplicationEventPublisher eventPublisher;

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
            then(eventPublisher).should()
                    .publishEvent(GameBoardMembersChangedEvent.membersOnly(BOARD_ID, MEMBER_ID));
        }

        @Test
        @DisplayName("게임에 배정된 회원에게 게임 시작 알림 이벤트를 발행한다")
        void startGame_publishesGameStartedEventForMemberPlayers() {
            // given - 회원 계정이 연결된 명단 멤버 2명으로 대기 게임 구성
            Member account20 = Member.builder().id(200L).build();
            Member account30 = Member.builder().id(300L).build();
            GamePlayer p1 = GameFixture.player(
                    GameFixture.memberWithAccount(1L, board, account20, "빠나영", Level.A), 0);
            GamePlayer p2 = GameFixture.player(
                    GameFixture.memberWithAccount(2L, board, account30, "김민지", Level.B), 1);
            Game waiting = GameFixture.waitingGame(GAME_ID, board, 1, p1, p2);

            Party party = Party.builder().id(10L).partyName("우리모임").build();
            Exercise exercise = Exercise.builder().party(party).build();

            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findById(GAME_ID)).willReturn(Optional.of(waiting));
            given(courtRepository.findByIdAndGameBoardId(COURT_ID, BOARD_ID)).willReturn(Optional.of(court));
            given(gameRepository.findByCourtIdAndStatus(COURT_ID, GameStatus.PLAYING)).willReturn(Optional.empty());
            given(gameRepository.findByGameBoardIdAndStatusOrderByWaitingOrderAsc(BOARD_ID, GameStatus.WAITING))
                    .willReturn(List.of());
            given(exerciseRepository.findByGameBoardId(BOARD_ID)).willReturn(Optional.of(exercise));

            // when
            gameCommandService.startGame(MEMBER_ID, new GameStartCommand(BOARD_ID, GAME_ID, COURT_ID));

            // then
            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            then(eventPublisher).should(times(2)).publishEvent(captor.capture());
            GameStartedEvent started = captor.getAllValues().stream()
                    .filter(GameStartedEvent.class::isInstance)
                    .map(GameStartedEvent.class::cast)
                    .findFirst().orElseThrow();
            assertThat(started.gameBoardId()).isEqualTo(BOARD_ID);
            assertThat(started.partyName()).isEqualTo("우리모임");
            assertThat(started.courtName()).isEqualTo("1번 코트");
            assertThat(started.recipientMemberIds()).containsExactly(200L, 300L);
        }

        @Test
        @DisplayName("게임 참가자가 전원 게스트면 게임 시작 알림 이벤트를 발행하지 않는다")
        void startGame_doesNotPublishWhenAllGuests() {
            // given - GameFixture.member 는 회원 계정(member)이 없는 게스트성 명단 멤버다
            GamePlayer guest = GameFixture.player(
                    GameFixture.member(1L, board, "게스트", Level.A), 0);
            Game waiting = GameFixture.waitingGame(GAME_ID, board, 1, guest);
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findById(GAME_ID)).willReturn(Optional.of(waiting));
            given(courtRepository.findByIdAndGameBoardId(COURT_ID, BOARD_ID)).willReturn(Optional.of(court));
            given(gameRepository.findByCourtIdAndStatus(COURT_ID, GameStatus.PLAYING)).willReturn(Optional.empty());
            given(gameRepository.findByGameBoardIdAndStatusOrderByWaitingOrderAsc(BOARD_ID, GameStatus.WAITING))
                    .willReturn(List.of());

            // when
            gameCommandService.startGame(MEMBER_ID, new GameStartCommand(BOARD_ID, GAME_ID, COURT_ID));

            // then
            then(eventPublisher).should(never()).publishEvent(any(GameStartedEvent.class));
            then(exerciseRepository).shouldHaveNoInteractions();
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
            given(gameBoardReader.readForUpdate(BOARD_ID)).willReturn(board);
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
            then(eventPublisher).should()
                    .publishEvent(GameBoardMembersChangedEvent.membersOnly(BOARD_ID, MEMBER_ID));
        }

        @Test
        @DisplayName("게임 진행자가 아니면 GAME_BOARD_ACCESS_DENIED 예외로 게임을 만들지 않는다")
        void createGame_deniesNonHost() {
            given(gameBoardReader.readForUpdate(BOARD_ID)).willReturn(board);
            willThrow(new GameException(GameErrorCode.GAME_BOARD_ACCESS_DENIED))
                    .given(gameBoardAccessValidator).validateGameHost(BOARD_ID, MEMBER_ID);

            assertThatThrownBy(() -> gameCommandService.createGame(
                    MEMBER_ID, new GameCreateCommand(BOARD_ID, List.of(7L))))
                    .isInstanceOf(GameException.class)
                    .extracting(e -> ((GameException) e).getCode())
                    .isEqualTo(GameErrorCode.GAME_BOARD_ACCESS_DENIED);
            then(gameRepository).should(never()).save(any());
            then(eventPublisher).should(never()).publishEvent(any());
        }

        @Test
        @DisplayName("명단에 없는 멤버가 포함되면 GAME_BOARD_MEMBER_NOT_FOUND 예외")
        void createGame_memberNotOnBoard() {
            GameBoardMember m7 = GameFixture.member(7L, board, "선수7", Level.A);
            given(gameBoardReader.readForUpdate(BOARD_ID)).willReturn(board);
            given(gameBoardMemberRepository.findByGameBoardIdAndIdIn(BOARD_ID, List.of(7L, 999L)))
                    .willReturn(List.of(m7)); // 999는 조회되지 않음

            assertThatThrownBy(() -> gameCommandService.createGame(MEMBER_ID, new GameCreateCommand(BOARD_ID, List.of(7L, 999L))))
                    .isInstanceOf(GameException.class)
                    .extracting(e -> ((GameException) e).getCode())
                    .isEqualTo(GameErrorCode.GAME_BOARD_MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("불참 상태의 선수가 포함되면 INACTIVE_GAME_PLAYER 예외")
        void createGame_inactivePlayer() {
            GameBoardMember inactiveMember = GameFixture.member(7L, board, "불참 선수", Level.A);
            inactiveMember.changeParticipation(false);
            given(gameBoardReader.readForUpdate(BOARD_ID)).willReturn(board);
            given(gameBoardMemberRepository.findByGameBoardIdAndIdIn(BOARD_ID, List.of(7L)))
                    .willReturn(List.of(inactiveMember));

            assertThatThrownBy(() -> gameCommandService.createGame(
                    MEMBER_ID, new GameCreateCommand(BOARD_ID, List.of(7L))))
                    .isInstanceOf(GameException.class)
                    .extracting(e -> ((GameException) e).getCode())
                    .isEqualTo(GameErrorCode.INACTIVE_GAME_PLAYER);
            then(gameRepository).should(never()).save(any());
            then(eventPublisher).should(never()).publishEvent(any());
        }

        @Test
        @DisplayName("이미 대기(WAITING) 게임에 편성된 선수가 포함되면 UNAVAILABLE_GAME_PLAYER 예외")
        void createGame_unavailablePlayer() {
            GameBoardMember member = GameFixture.member(7L, board, "선택 불가", Level.A);
            given(gameBoardReader.readForUpdate(BOARD_ID)).willReturn(board);
            given(gameBoardMemberRepository.findByGameBoardIdAndIdIn(BOARD_ID, List.of(7L)))
                    .willReturn(List.of(member));
            given(availabilityPolicy.hasWaitingConflict(any(), any())).willReturn(true);

            assertThatThrownBy(() -> gameCommandService.createGame(
                    MEMBER_ID, new GameCreateCommand(BOARD_ID, List.of(7L))))
                    .isInstanceOfSatisfying(GameException.class, exception ->
                            assertThat(exception.getCode())
                                    .isEqualTo(GameErrorCode.UNAVAILABLE_GAME_PLAYER));

            then(gameRepository).should(never()).save(any());
            then(eventPublisher).should(never()).publishEvent(any());
        }

        @Test
        @DisplayName("급수없음 선수도 수동 선택에서는 게임에 추가할 수 있다")
        void createGame_allowsPlayerWithoutLevelWhenManuallySelected() {
            GameBoardMember member = GameFixture.member(7L, board, "급수없음", Level.NONE);
            given(gameBoardReader.readForUpdate(BOARD_ID)).willReturn(board);
            given(gameBoardMemberRepository.findByGameBoardIdAndIdIn(BOARD_ID, List.of(7L)))
                    .willReturn(List.of(member));
            given(gameRepository.save(any(Game.class))).willAnswer(invocation -> invocation.getArgument(0));

            gameCommandService.createGame(
                    MEMBER_ID, new GameCreateCommand(BOARD_ID, List.of(7L)));

            then(gameRepository).should().save(any(Game.class));
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
    @DisplayName("deleteGame (#6 게임 취소/대기 삭제)")
    class DeleteGame {

        @Test
        @DisplayName("대기 게임을 삭제하면 삭제 후 남은 대기열 순서를 재정렬한다")
        void deleteGame_waitingResequences() {
            Game waiting = GameFixture.waitingGame(GAME_ID, board, 1);
            Game remaining = GameFixture.waitingGame(51L, board, 2);
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findById(GAME_ID)).willReturn(Optional.of(waiting));
            given(gameRepository.findByGameBoardIdAndStatusOrderByWaitingOrderAsc(BOARD_ID, GameStatus.WAITING))
                    .willReturn(List.of(remaining));

            GameDeleteResult result = gameCommandService.deleteGame(MEMBER_ID, new GameDeleteCommand(BOARD_ID, GAME_ID, false));

            assertThat(result.gameId()).isEqualTo(GAME_ID);
            then(gameRepository).should().delete(waiting);
            assertThat(remaining.getWaitingOrder()).isEqualTo(1);
            then(eventPublisher).should()
                    .publishEvent(GameBoardMembersChangedEvent.membersOnly(BOARD_ID, MEMBER_ID));
        }

        @Test
        @DisplayName("진행 게임을 취소하면 삭제만 하고 대기열 재정렬은 하지 않는다")
        void deleteGame_playingNoResequence() {
            Game playing = GameFixture.playingGame(GAME_ID, board, court, LocalDateTime.now());
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findById(GAME_ID)).willReturn(Optional.of(playing));

            gameCommandService.deleteGame(MEMBER_ID, new GameDeleteCommand(BOARD_ID, GAME_ID, false));

            then(gameRepository).should().delete(playing);
            then(gameRepository).should(never())
                    .findByGameBoardIdAndStatusOrderByWaitingOrderAsc(any(), any());
        }

        @Test
        @DisplayName("restore=true면 삭제된 게임의 플레이어를 playerOrder 순서로 반환한다")
        void deleteGame_restoreReturnsPlayers() {
            GameBoardMember m7 = GameFixture.member(7L, board, "선수7", Level.A);
            GameBoardMember m8 = GameFixture.member(8L, board, "선수8", Level.SEMI_EXPERT);
            // 입력 순서를 뒤집어 정렬 검증
            Game waiting = GameFixture.waitingGame(GAME_ID, board, 1,
                    GameFixture.player(m8, 1), GameFixture.player(m7, 0));
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findById(GAME_ID)).willReturn(Optional.of(waiting));
            given(gameRepository.findByGameBoardIdAndStatusOrderByWaitingOrderAsc(BOARD_ID, GameStatus.WAITING))
                    .willReturn(List.of());

            GameDeleteResult result = gameCommandService.deleteGame(MEMBER_ID, new GameDeleteCommand(BOARD_ID, GAME_ID, true));

            assertThat(result.players())
                    .extracting(GameDeleteResult.PlayerView::gameBoardMemberId, GameDeleteResult.PlayerView::playerOrder)
                    .containsExactly(tuple(7L, 0), tuple(8L, 1));
            assertThat(result.players().get(0).name()).isEqualTo("선수7");
            assertThat(result.players().get(0).level()).isEqualTo(Level.A);
        }

        @Test
        @DisplayName("restore=false면 플레이어 목록은 비어 있다")
        void deleteGame_noRestoreEmptyPlayers() {
            GameBoardMember m7 = GameFixture.member(7L, board, "선수7", Level.A);
            Game waiting = GameFixture.waitingGame(GAME_ID, board, 1, GameFixture.player(m7, 0));
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findById(GAME_ID)).willReturn(Optional.of(waiting));
            given(gameRepository.findByGameBoardIdAndStatusOrderByWaitingOrderAsc(BOARD_ID, GameStatus.WAITING))
                    .willReturn(List.of());

            GameDeleteResult result = gameCommandService.deleteGame(MEMBER_ID, new GameDeleteCommand(BOARD_ID, GAME_ID, false));

            assertThat(result.players()).isEmpty();
        }

        @Test
        @DisplayName("게임을 찾을 수 없으면 GAME_NOT_FOUND 예외")
        void deleteGame_gameNotFound() {
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findById(GAME_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> gameCommandService.deleteGame(MEMBER_ID, new GameDeleteCommand(BOARD_ID, GAME_ID, false)))
                    .isInstanceOf(GameException.class)
                    .extracting(e -> ((GameException) e).getCode())
                    .isEqualTo(GameErrorCode.GAME_NOT_FOUND);
        }

        @Test
        @DisplayName("이미 완료된 게임은 GAME_ALREADY_COMPLETED 예외")
        void deleteGame_alreadyCompleted() {
            Game completed = Game.builder()
                    .id(GAME_ID).gameBoard(board).status(GameStatus.COMPLETED).build();
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findById(GAME_ID)).willReturn(Optional.of(completed));

            assertThatThrownBy(() -> gameCommandService.deleteGame(MEMBER_ID, new GameDeleteCommand(BOARD_ID, GAME_ID, false)))
                    .isInstanceOf(GameException.class)
                    .extracting(e -> ((GameException) e).getCode())
                    .isEqualTo(GameErrorCode.GAME_ALREADY_COMPLETED);
        }
    }

    @Nested
    @DisplayName("moveGameToWaiting (#7 대기열 이동)")
    class MoveGameToWaiting {

        @Test
        @DisplayName("진행 게임을 기록 없이 같은 게임 그대로 대기열 맨 앞으로 되돌린다 (완료 아님, 게임횟수 불변)")
        void moveGameToWaiting_returnsSameGameToFrontWithoutRecording() {
            // given
            GameBoardMember m1 = GameFixture.member(7L, board, "선수A", Level.A);
            GameBoardMember m2 = GameFixture.member(8L, board, "선수B", Level.B);
            Game playing = GameFixture.playingGame(GAME_ID, board, court, LocalDateTime.now(),
                    GameFixture.player(m1, 0), GameFixture.player(m2, 1));
            Game otherWaiting = GameFixture.waitingGame(60L, board, 1);
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findById(GAME_ID)).willReturn(Optional.of(playing));
            // 재정렬: 되돌린 게임(임시순서 0) → 1번, 기존 대기 게임 → 2번
            given(gameRepository.findByGameBoardIdAndStatusOrderByWaitingOrderAsc(BOARD_ID, GameStatus.WAITING))
                    .willReturn(List.of(playing, otherWaiting));

            // when
            gameCommandService.moveGameToWaiting(MEMBER_ID, new GameToWaitingCommand(BOARD_ID, GAME_ID));

            // then - 같은 게임이 WAITING으로 되돌아가고 코트가 빈다. 완료/게임횟수 증가는 없다.
            assertThat(playing.getStatus()).isEqualTo(GameStatus.WAITING);
            assertThat(playing.getCourt()).isNull();
            assertThat(playing.getStartedAt()).isNull();
            assertThat(playing.getCompletedAt()).isNull();
            assertThat(playing.getWaitingOrder()).isEqualTo(1);
            assertThat(otherWaiting.getWaitingOrder()).isEqualTo(2);
            assertThat(m1.getGameCount()).isZero();
            assertThat(m2.getGameCount()).isZero();
            assertThat(playing.getPlayers())
                    .extracting(p -> p.getGameBoardMember().getId(), GamePlayer::getPlayerOrder)
                    .containsExactly(tuple(7L, 0), tuple(8L, 1));

            // 새 게임을 만들지 않는다 (같은 게임을 재사용)
            then(gameRepository).should(never()).save(any(Game.class));
            then(eventPublisher).should()
                    .publishEvent(GameBoardMembersChangedEvent.membersOnly(BOARD_ID, MEMBER_ID));
        }

        @Test
        @DisplayName("게임을 찾을 수 없으면 GAME_NOT_FOUND 예외")
        void moveGameToWaiting_gameNotFound() {
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findById(GAME_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> gameCommandService.moveGameToWaiting(MEMBER_ID, new GameToWaitingCommand(BOARD_ID, GAME_ID)))
                    .isInstanceOf(GameException.class)
                    .extracting(e -> ((GameException) e).getCode())
                    .isEqualTo(GameErrorCode.GAME_NOT_FOUND);
        }

        @Test
        @DisplayName("진행 중인 게임이 아니면 GAME_NOT_PLAYING 예외")
        void moveGameToWaiting_notPlaying() {
            Game waiting = GameFixture.waitingGame(GAME_ID, board, 1);
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findById(GAME_ID)).willReturn(Optional.of(waiting));

            assertThatThrownBy(() -> gameCommandService.moveGameToWaiting(MEMBER_ID, new GameToWaitingCommand(BOARD_ID, GAME_ID)))
                    .isInstanceOf(GameException.class)
                    .extracting(e -> ((GameException) e).getCode())
                    .isEqualTo(GameErrorCode.GAME_NOT_PLAYING);
        }
    }

    @Nested
    @DisplayName("completeGame (게임 완료)")
    class CompleteGame {

        @Test
        @DisplayName("진행 게임을 완료 처리하고 참여자 게임횟수를 +1 하며 코트를 비운다 (시작시각 보존)")
        void completeGame_completesAndIncrementsCount() {
            // given
            GameBoardMember m1 = GameFixture.member(7L, board, "선수A", Level.A);
            GameBoardMember m2 = GameFixture.member(8L, board, "선수B", Level.B);
            LocalDateTime startedAt = LocalDateTime.now().minusMinutes(15);
            Game playing = GameFixture.playingGame(GAME_ID, board, court, startedAt,
                    GameFixture.player(m1, 0), GameFixture.player(m2, 1));
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findById(GAME_ID)).willReturn(Optional.of(playing));

            // when
            gameCommandService.completeGame(MEMBER_ID, new GameCompleteCommand(BOARD_ID, GAME_ID));

            // then - 완료 상태 + 완료시각 저장, 코트는 비우되 코트 번호는 스냅샷으로 보존
            assertThat(playing.getStatus()).isEqualTo(GameStatus.COMPLETED);
            assertThat(playing.getCourt()).isNull();
            assertThat(playing.getCourtNo()).isEqualTo(court.getCourtNo());
            assertThat(playing.getCompletedAt()).isNotNull();
            // 경과시각 계산용으로 시작시각은 유지된다
            assertThat(playing.getStartedAt()).isEqualTo(startedAt);
            assertThat(m1.getGameCount()).isEqualTo(1);
            assertThat(m2.getGameCount()).isEqualTo(1);
            then(eventPublisher).should()
                    .publishEvent(GameBoardMembersChangedEvent.membersOnly(BOARD_ID, MEMBER_ID));
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
            Game playingOfOther = GameFixture.playingGame(GAME_ID, otherBoard, court, LocalDateTime.now());
            given(gameBoardReader.read(BOARD_ID)).willReturn(board);
            given(gameRepository.findById(GAME_ID)).willReturn(Optional.of(playingOfOther));

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
