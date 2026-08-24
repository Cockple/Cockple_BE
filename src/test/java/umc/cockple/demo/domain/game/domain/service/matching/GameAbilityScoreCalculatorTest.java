package umc.cockple.demo.domain.game.domain.service.matching;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.enums.AgeGroup;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GameAbilityScoreCalculator")
class GameAbilityScoreCalculatorTest {

    private final GameAbilityScoreCalculator calculator = new GameAbilityScoreCalculator();

    @ParameterizedTest(name = "{0} 기본 점수는 {1}점이다")
    @MethodSource("validLevelScores")
    @DisplayName("전체 유효 급수의 기본 점수를 10배 정수로 계산한다")
    void calculate_appliesLevelScore(Level level, int expectedScore) {
        GameBoardMember member = member(Gender.MALE, level, AgeGroup.FIFTIES);

        assertThat(calculator.calculate(member)).isEqualTo(expectedScore);
    }

    @ParameterizedTest(name = "남성 {0}의 나이 보정은 {1}점이다")
    @CsvSource({
            "TEENS, 6",
            "TWENTIES, 6",
            "THIRTIES, 4",
            "FORTIES, 2",
            "FIFTIES, 0",
            "SIXTIES, 0",
            "SEVENTIES, 0"
    })
    @DisplayName("남성 연령대별 보정을 적용한다")
    void calculate_appliesMaleAgeBonus(AgeGroup ageGroup, int expectedBonus) {
        GameBoardMember member = member(Gender.MALE, Level.A, ageGroup);

        assertThat(calculator.calculate(member)).isEqualTo(80 + expectedBonus);
    }

    @Test
    @DisplayName("연령대가 없는 남성은 30대와 동일한 보정을 적용한다")
    void calculate_appliesDefaultMaleAgeBonus() {
        GameBoardMember member = member(Gender.MALE, Level.A, null);

        assertThat(calculator.calculate(member)).isEqualTo(84);
    }

    @ParameterizedTest(name = "여성 {0}도 나이와 관계없이 10점 감점한다")
    @EnumSource(AgeGroup.class)
    @DisplayName("여성은 연령대와 관계없이 기본 점수에서 10점을 감점한다")
    void calculate_appliesFemalePenalty(AgeGroup ageGroup) {
        GameBoardMember member = member(Gender.FEMALE, Level.A, ageGroup);

        assertThat(calculator.calculate(member)).isEqualTo(70);
    }

    @Test
    @DisplayName("급수없음 선수에게는 능력치 점수를 부여하지 않는다")
    void calculate_rejectsMemberWithoutLevel() {
        GameBoardMember member = member(Gender.MALE, Level.NONE, AgeGroup.TWENTIES);

        assertThatThrownBy(() -> calculator.calculate(member))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<Arguments> validLevelScores() {
        return Stream.of(
                Arguments.of(Level.EXPERT, 100),
                Arguments.of(Level.SEMI_EXPERT, 90),
                Arguments.of(Level.A, 80),
                Arguments.of(Level.B, 70),
                Arguments.of(Level.C, 60),
                Arguments.of(Level.D, 50),
                Arguments.of(Level.BEGINNER, 40),
                Arguments.of(Level.NOVICE, 30));
    }

    private GameBoardMember member(Gender gender, Level level, AgeGroup ageGroup) {
        return GameBoardMember.builder()
                .gender(gender)
                .level(level)
                .ageGroup(ageGroup)
                .build();
    }
}
