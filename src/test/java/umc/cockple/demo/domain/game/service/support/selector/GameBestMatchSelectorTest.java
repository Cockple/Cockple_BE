package umc.cockple.demo.domain.game.service.support.selector;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.domain.GamePlayer;
import umc.cockple.demo.domain.game.enums.AgeGroup;
import umc.cockple.demo.domain.game.enums.GameMatchType;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.service.support.calculator.GameAbilityScoreCalculator;
import umc.cockple.demo.domain.game.service.support.calculator.GamePairHistoryCalculator;
import umc.cockple.demo.domain.game.service.support.calculator.GamePairHistoryCalculator.GamePairHistory;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.GameFixture;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GameBestMatchSelector")
class GameBestMatchSelectorTest {

    private final GamePairHistoryCalculator pairHistoryCalculator =
            new GamePairHistoryCalculator();
    private final GameBestMatchSelector selector =
            new GameBestMatchSelector(new GameAbilityScoreCalculator());
    private final GamePairHistory emptyHistory = pairHistoryCalculator.calculate(List.of());
    private final GameBoard board = GameFixture.gameBoard(1L);

    @Test
    @DisplayName("동일 성별 복식은 세 가지 팀 분할 중 점수 차이가 가장 작은 조합을 사용한다")
    void select_comparesAllSameGenderTeamSplits() {
        List<GameBoardMember> candidates = List.of(
                member(1L, Gender.MALE, Level.EXPERT, AgeGroup.FIFTIES, 0),
                member(2L, Gender.MALE, Level.SEMI_EXPERT, AgeGroup.FIFTIES, 0),
                member(3L, Gender.MALE, Level.A, AgeGroup.FIFTIES, 0),
                member(4L, Gender.MALE, Level.NOVICE, AgeGroup.FIFTIES, 0),
                member(5L, Gender.MALE, Level.NOVICE, AgeGroup.FIFTIES, 0));

        List<Long> result = selector.select(
                candidates, GameMatchType.MEN_DOUBLES, emptyHistory);

        assertThat(result).containsExactly(1L, 2L, 4L, 5L);
    }

    @Test
    @DisplayName("혼복은 각 팀이 남녀 한 명씩인 두 가지 분할만 비교한다")
    void select_comparesOnlyMixedGenderTeamSplits() {
        List<GameBoardMember> candidates = List.of(
                member(1L, Gender.MALE, Level.EXPERT, AgeGroup.FIFTIES, 0),
                member(2L, Gender.MALE, Level.C, AgeGroup.FIFTIES, 0),
                member(3L, Gender.MALE, Level.A, AgeGroup.FIFTIES, 0),
                member(4L, Gender.FEMALE, Level.EXPERT, AgeGroup.FIFTIES, 0),
                member(5L, Gender.FEMALE, Level.A, AgeGroup.FIFTIES, 0));

        List<Long> result = selector.select(
                candidates, GameMatchType.MIXED_DOUBLES, emptyHistory);

        assertThat(result).containsExactly(1L, 3L, 4L, 5L);
    }

    @Test
    @DisplayName("밸런스 점수와 중복 점수에 각각 10과 15의 가중치를 적용한다")
    void select_appliesBalanceAndDuplicateWeights() {
        List<GameBoardMember> candidates = weightedScoreCandidates(0, 0, 0, 0, 0);
        GamePairHistory pairHistory = historyOf(candidates.get(1), candidates.get(2));

        List<Long> result = selector.select(
                candidates, GameMatchType.MEN_DOUBLES, pairHistory);

        assertThat(result).containsExactly(1L, 2L, 3L, 4L);
    }

    @Test
    @DisplayName("4명 사이 여섯 pair의 완료 경기 중복 횟수를 모두 합산한다")
    void select_sumsAllPairHistoryCounts() {
        List<GameBoardMember> candidates = List.of(
                member(1L, Gender.MALE, Level.A, AgeGroup.FIFTIES, 0),
                member(2L, Gender.MALE, Level.A, AgeGroup.FIFTIES, 0),
                member(3L, Gender.MALE, Level.A, AgeGroup.FIFTIES, 0),
                member(4L, Gender.MALE, Level.A, AgeGroup.FIFTIES, 0),
                member(5L, Gender.MALE, Level.A, AgeGroup.FIFTIES, 0));
        GamePairHistory pairHistory = historyOf(
                candidates.get(0), candidates.get(1), candidates.get(2), candidates.get(3));

        List<Long> result = selector.select(
                candidates, GameMatchType.MEN_DOUBLES, pairHistory);

        assertThat(result).containsExactly(1L, 2L, 3L, 5L);
    }

    @Test
    @DisplayName("최종 점수가 같으면 중복 점수가 낮은 조합을 우선한다")
    void select_usesDuplicateScoreAsFirstTieBreaker() {
        List<GameBoardMember> candidates = List.of(
                member(1L, Gender.MALE, Level.A, AgeGroup.FIFTIES, 0),
                member(2L, Gender.MALE, Level.A, AgeGroup.FIFTIES, 0),
                member(3L, Gender.MALE, Level.A, AgeGroup.FIFTIES, 0),
                member(4L, Gender.MALE, Level.A, AgeGroup.FIFTIES, 0),
                member(5L, Gender.MALE, Level.A, AgeGroup.FIFTIES, 15));
        GamePairHistory pairHistory = historyOf(candidates.get(1), candidates.get(2));

        List<Long> result = selector.select(
                candidates, GameMatchType.MEN_DOUBLES, pairHistory);

        assertThat(result).containsExactly(1L, 2L, 4L, 5L);
    }

    @Test
    @DisplayName("최종 점수와 중복 점수가 같으면 경기 수 합이 낮은 조합을 우선한다")
    void select_usesFairnessScoreAsSecondTieBreaker() {
        List<GameBoardMember> candidates = weightedScoreCandidates(0, 20, 0, 0, 0);

        List<Long> result = selector.select(
                candidates, GameMatchType.MEN_DOUBLES, emptyHistory);

        assertThat(result).containsExactly(1L, 3L, 4L, 5L);
    }

    @Test
    @DisplayName("모든 점수가 같으면 정렬된 명단 ID가 빠른 조합을 우선한다")
    void select_usesMemberIdsAsFinalTieBreaker() {
        List<GameBoardMember> candidates = List.of(
                member(5L, Gender.MALE, Level.A, AgeGroup.FIFTIES, 0),
                member(2L, Gender.MALE, Level.A, AgeGroup.FIFTIES, 0),
                member(4L, Gender.MALE, Level.A, AgeGroup.FIFTIES, 0),
                member(1L, Gender.MALE, Level.A, AgeGroup.FIFTIES, 0),
                member(3L, Gender.MALE, Level.A, AgeGroup.FIFTIES, 0));

        List<Long> result = selector.select(
                candidates, GameMatchType.MEN_DOUBLES, emptyHistory);

        assertThat(result).containsExactly(1L, 2L, 3L, 4L);
    }

    @Test
    @DisplayName("매치 타입에 맞는 4인 조합이 없으면 GAME417 예외를 던진다")
    void select_rejectsWhenNoCombinationMatchesType() {
        List<GameBoardMember> candidates = List.of(
                member(1L, Gender.MALE, Level.A, AgeGroup.FIFTIES, 0),
                member(2L, Gender.MALE, Level.A, AgeGroup.FIFTIES, 0),
                member(3L, Gender.MALE, Level.A, AgeGroup.FIFTIES, 0),
                member(4L, Gender.FEMALE, Level.A, AgeGroup.FIFTIES, 0));

        assertThatThrownBy(() -> selector.select(
                candidates, GameMatchType.MIXED_DOUBLES, emptyHistory))
                .isInstanceOfSatisfying(GameException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo(GameErrorCode.RANDOM_MATCH_NOT_FOUND));
    }

    private List<GameBoardMember> weightedScoreCandidates(
            int firstGameCount,
            int secondGameCount,
            int thirdGameCount,
            int fourthGameCount,
            int fifthGameCount) {
        return List.of(
                member(1L, Gender.MALE, Level.NOVICE, AgeGroup.FIFTIES, firstGameCount),
                member(2L, Gender.MALE, Level.NOVICE, AgeGroup.FORTIES, secondGameCount),
                member(3L, Gender.MALE, Level.NOVICE, AgeGroup.THIRTIES, thirdGameCount),
                member(4L, Gender.MALE, Level.NOVICE, AgeGroup.TWENTIES, fourthGameCount),
                member(5L, Gender.MALE, Level.BEGINNER, AgeGroup.FORTIES, fifthGameCount));
    }

    private GamePairHistory historyOf(GameBoardMember... members) {
        GamePlayer[] players = IntStream.range(0, members.length)
                .mapToObj(index -> GameFixture.player(members[index], index))
                .toArray(GamePlayer[]::new);
        Game completedGame = GameFixture.completedGame(
                1L, board, LocalDateTime.of(2026, 8, 24, 10, 0), players);
        return pairHistoryCalculator.calculate(List.of(completedGame));
    }

    private GameBoardMember member(
            Long id,
            Gender gender,
            Level level,
            AgeGroup ageGroup,
            int gameCount) {
        return GameBoardMember.builder()
                .id(id)
                .gameBoard(board)
                .name("선수" + id)
                .gender(gender)
                .level(level)
                .ageGroup(ageGroup)
                .participating(true)
                .gameCount(gameCount)
                .shuttlecockSubmitted(false)
                .build();
    }
}
