package umc.cockple.demo.domain.game.service.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.domain.service.GamePairCount;
import umc.cockple.demo.domain.game.enums.AgeGroup;
import umc.cockple.demo.domain.game.enums.GameMatchType;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.repository.GameBoardMemberRepository;
import umc.cockple.demo.domain.game.repository.GameRepository;
import umc.cockple.demo.domain.game.service.query.result.GameRandomMatchResult;
import umc.cockple.demo.domain.game.domain.service.GamePairHistoryCalculator;
import umc.cockple.demo.domain.game.domain.service.GamePairHistoryCalculator.GamePairHistory;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;
import umc.cockple.demo.domain.game.domain.service.matching.GameBestMatchSelector;
import umc.cockple.demo.domain.game.domain.service.matching.GameCandidatePoolSelector;
import umc.cockple.demo.domain.game.domain.service.matching.GameMatchTypeSelector;
import umc.cockple.demo.domain.game.service.support.validator.GameBoardAccessValidator;
import umc.cockple.demo.domain.game.domain.service.GameBoardMemberAvailabilityPolicy;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.GameFixture;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameRandomMatchQueryService")
class GameRandomMatchQueryServiceTest {

    private static final Long MEMBER_ID = 10L;
    private static final Long BOARD_ID = 1L;
    private static final List<GameStatus> ACTIVE_STATUSES =
            List.of(GameStatus.WAITING, GameStatus.PLAYING);

    @Mock private GameBoardReader gameBoardReader;
    @Mock private GameBoardMemberRepository gameBoardMemberRepository;
    @Mock private GameRepository gameRepository;
    @Mock private GameBoardAccessValidator gameBoardAccessValidator;
    @Mock private GameBoardMemberAvailabilityPolicy availabilityPolicy;
    @Mock private GameMatchTypeSelector matchTypeSelector;
    @Mock private GameCandidatePoolSelector candidatePoolSelector;
    @Mock private GamePairHistoryCalculator pairHistoryCalculator;
    @Mock private GameBestMatchSelector bestMatchSelector;

    private GameRandomMatchQueryService service;
    private GameBoard board;

    @BeforeEach
    void setUp() {
        board = GameFixture.gameBoard(BOARD_ID);
        service = new GameRandomMatchQueryService(
                gameBoardReader,
                gameBoardMemberRepository,
                gameRepository,
                gameBoardAccessValidator,
                availabilityPolicy,
                matchTypeSelector,
                candidatePoolSelector,
                pairHistoryCalculator,
                bestMatchSelector);
    }

    @Test
    @DisplayName("가용한 급수 보유 선수로 후보 풀과 최적 조합을 순서대로 계산한다")
    void match_orchestratesRandomMatching() {
        List<GameBoardMember> members = List.of(
                member(1L, Level.NONE),
                member(2L, Level.A),
                member(3L, Level.B),
                member(4L, Level.C),
                member(5L, Level.D));
        List<GameBoardMember> candidates = members.subList(1, 5);
        List<GameBoardMember> candidatePool = List.copyOf(candidates);
        List<Game> activeGames = List.of();
        List<GamePairCount> pairCounts = List.of();
        GamePairHistory pairHistory = new GamePairHistoryCalculator().calculate(List.of());

        given(gameBoardReader.read(BOARD_ID)).willReturn(board);
        given(gameBoardMemberRepository.findByGameBoardIdOrderByIdAsc(BOARD_ID))
                .willReturn(members);
        given(gameRepository.findByGameBoardIdAndStatusInWithPlayers(
                BOARD_ID, ACTIVE_STATUSES)).willReturn(activeGames);
        given(availabilityPolicy.filterAvailable(
                eq(members), eq(activeGames), any(LocalDateTime.class))).willReturn(members);
        given(matchTypeSelector.findAvailableTypes(candidates))
                .willReturn(List.of(GameMatchType.MEN_DOUBLES));
        given(candidatePoolSelector.find(candidates, GameMatchType.MEN_DOUBLES))
                .willReturn(Optional.of(candidatePool));
        given(matchTypeSelector.selectFrom(List.of(GameMatchType.MEN_DOUBLES)))
                .willReturn(GameMatchType.MEN_DOUBLES);
        given(gameRepository.countCompletedGamePairs(
                BOARD_ID, List.of(2L, 3L, 4L, 5L))).willReturn(pairCounts);
        given(pairHistoryCalculator.fromCounts(pairCounts, List.of())).willReturn(pairHistory);
        given(bestMatchSelector.select(
                candidatePool, GameMatchType.MEN_DOUBLES, pairHistory))
                .willReturn(List.of(2L, 3L, 4L, 5L));

        GameRandomMatchResult result = service.match(MEMBER_ID, BOARD_ID);

        assertThat(result.gameBoardMemberIds()).containsExactly(2L, 3L, 4L, 5L);
        then(gameBoardAccessValidator).should().validateGameHost(BOARD_ID, MEMBER_ID);
        then(availabilityPolicy).should().filterAvailable(
                eq(members), eq(activeGames), any(LocalDateTime.class));
        then(matchTypeSelector).should().findAvailableTypes(candidates);
        then(candidatePoolSelector).should()
                .find(candidates, GameMatchType.MEN_DOUBLES);
        then(matchTypeSelector).should().selectFrom(List.of(GameMatchType.MEN_DOUBLES));
        then(gameRepository).should().countCompletedGamePairs(
                BOARD_ID, List.of(2L, 3L, 4L, 5L));
        then(pairHistoryCalculator).should().fromCounts(pairCounts, List.of());
        then(bestMatchSelector).should()
                .select(candidatePool, GameMatchType.MEN_DOUBLES, pairHistory);
        then(gameRepository).should(never()).save(any(Game.class));
        then(gameBoardMemberRepository).should(never()).save(any(GameBoardMember.class));
    }

    @Test
    @DisplayName("가용한 급수 보유 선수가 4명 미만이면 GAME415 예외를 던진다")
    void match_rejectsInsufficientAvailablePlayers() {
        List<GameBoardMember> members = List.of(
                member(1L, Level.A),
                member(2L, Level.B),
                member(3L, Level.C),
                member(4L, Level.NONE));
        List<Game> activeGames = List.of();

        given(gameBoardReader.read(BOARD_ID)).willReturn(board);
        given(gameBoardMemberRepository.findByGameBoardIdOrderByIdAsc(BOARD_ID))
                .willReturn(members);
        given(gameRepository.findByGameBoardIdAndStatusInWithPlayers(
                BOARD_ID, ACTIVE_STATUSES)).willReturn(activeGames);
        given(availabilityPolicy.filterAvailable(
                eq(members), eq(activeGames), any(LocalDateTime.class))).willReturn(members);

        assertThatThrownBy(() -> service.match(MEMBER_ID, BOARD_ID))
                .isInstanceOfSatisfying(GameException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo(GameErrorCode.INSUFFICIENT_AVAILABLE_PLAYERS));

        then(matchTypeSelector).should(never()).findAvailableTypes(anyList());
        then(candidatePoolSelector).shouldHaveNoInteractions();
        then(pairHistoryCalculator).shouldHaveNoInteractions();
        then(bestMatchSelector).shouldHaveNoInteractions();
        then(gameRepository).should(never()).countCompletedGamePairs(eq(BOARD_ID), anyList());
    }

    @Test
    @DisplayName("일부 타입만 경기 수 +5 안에서 구성 가능하면 해당 타입으로 매칭한다")
    void match_selectsAmongTypesWithFeasibleCandidatePool() {
        List<GameBoardMember> members = List.of(
                member(1L, Gender.MALE, Level.A, 0),
                member(2L, Gender.MALE, Level.A, 0),
                member(3L, Gender.MALE, Level.A, 0),
                member(4L, Gender.MALE, Level.A, 0),
                member(5L, Gender.FEMALE, Level.A, 6),
                member(6L, Gender.FEMALE, Level.A, 6));
        List<GameBoardMember> malePool = members.subList(0, 4);
        List<Game> activeGames = List.of();
        List<GamePairCount> pairCounts = List.of();
        GamePairHistory pairHistory = new GamePairHistoryCalculator().calculate(List.of());
        GameRandomMatchQueryService serviceWithRealSelectors = new GameRandomMatchQueryService(
                gameBoardReader,
                gameBoardMemberRepository,
                gameRepository,
                gameBoardAccessValidator,
                availabilityPolicy,
                new GameMatchTypeSelector(),
                new GameCandidatePoolSelector(),
                pairHistoryCalculator,
                bestMatchSelector);

        given(gameBoardReader.read(BOARD_ID)).willReturn(board);
        given(gameBoardMemberRepository.findByGameBoardIdOrderByIdAsc(BOARD_ID))
                .willReturn(members);
        given(gameRepository.findByGameBoardIdAndStatusInWithPlayers(
                BOARD_ID, ACTIVE_STATUSES)).willReturn(activeGames);
        given(availabilityPolicy.filterAvailable(
                eq(members), eq(activeGames), any(LocalDateTime.class))).willReturn(members);
        given(gameRepository.countCompletedGamePairs(
                BOARD_ID, List.of(1L, 2L, 3L, 4L))).willReturn(pairCounts);
        given(pairHistoryCalculator.fromCounts(pairCounts, List.of())).willReturn(pairHistory);
        given(bestMatchSelector.select(
                malePool, GameMatchType.MEN_DOUBLES, pairHistory))
                .willReturn(List.of(1L, 2L, 3L, 4L));

        GameRandomMatchResult result = serviceWithRealSelectors.match(MEMBER_ID, BOARD_ID);

        assertThat(result.gameBoardMemberIds()).containsExactly(1L, 2L, 3L, 4L);
        then(bestMatchSelector).should()
                .select(malePool, GameMatchType.MEN_DOUBLES, pairHistory);
    }

    private GameBoardMember member(Long id, Level level) {
        return member(id, Gender.MALE, level, 0);
    }

    private GameBoardMember member(Long id, Gender gender, Level level, int gameCount) {
        return GameBoardMember.builder()
                .id(id)
                .gameBoard(board)
                .name("선수" + id)
                .gender(gender)
                .level(level)
                .ageGroup(AgeGroup.TWENTIES)
                .participating(true)
                .gameCount(gameCount)
                .shuttlecockSubmitted(false)
                .build();
    }
}
