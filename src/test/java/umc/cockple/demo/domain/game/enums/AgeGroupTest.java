package umc.cockple.demo.domain.game.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AgeGroup")
class AgeGroupTest {

    private static final LocalDate EXERCISE_DATE = LocalDate.of(2030, 6, 15);

    @Test
    @DisplayName("10세부터 79세까지 운동일 기준 만 나이를 연령대로 변환한다")
    void fromBirthDate_convertsSupportedAges() {
        assertThat(ageGroupAt(10)).isEqualTo(AgeGroup.TEENS);
        assertThat(ageGroupAt(19)).isEqualTo(AgeGroup.TEENS);
        assertThat(ageGroupAt(20)).isEqualTo(AgeGroup.TWENTIES);
        assertThat(ageGroupAt(29)).isEqualTo(AgeGroup.TWENTIES);
        assertThat(ageGroupAt(30)).isEqualTo(AgeGroup.THIRTIES);
        assertThat(ageGroupAt(40)).isEqualTo(AgeGroup.FORTIES);
        assertThat(ageGroupAt(50)).isEqualTo(AgeGroup.FIFTIES);
        assertThat(ageGroupAt(60)).isEqualTo(AgeGroup.SIXTIES);
        assertThat(ageGroupAt(70)).isEqualTo(AgeGroup.SEVENTIES);
        assertThat(ageGroupAt(79)).isEqualTo(AgeGroup.SEVENTIES);
    }

    @Test
    @DisplayName("생일이 지나지 않았으면 한 살 어린 만 나이로 계산한다")
    void fromBirthDate_usesFullAgeAtExerciseDate() {
        LocalDate dayAfterBirthday = EXERCISE_DATE.minusYears(20).plusDays(1);

        assertThat(AgeGroup.fromBirthDate(dayAfterBirthday, EXERCISE_DATE))
                .isEqualTo(AgeGroup.TEENS);
    }

    @Test
    @DisplayName("지원 범위 밖이거나 날짜가 없으면 null을 반환한다")
    void fromBirthDate_returnsNullOutsideSupportedRange() {
        assertThat(ageGroupAt(9)).isNull();
        assertThat(ageGroupAt(80)).isNull();
        assertThat(AgeGroup.fromBirthDate(null, EXERCISE_DATE)).isNull();
        assertThat(AgeGroup.fromBirthDate(EXERCISE_DATE.minusYears(20), null)).isNull();
        assertThat(AgeGroup.fromBirthDate(EXERCISE_DATE.plusDays(1), EXERCISE_DATE)).isNull();
    }

    @Test
    @DisplayName("한글 연령대 문자열을 enum으로 변환한다")
    void fromKorean_convertsKoreanName() {
        assertThat(AgeGroup.fromKorean(" 30대 ")).isEqualTo(AgeGroup.THIRTIES);
    }

    @Test
    @DisplayName("지원하지 않는 연령대 문자열은 공통 형식 오류를 발생시킨다")
    void fromKorean_rejectsInvalidValue() {
        assertThatThrownBy(() -> AgeGroup.fromKorean("80대"))
                .isInstanceOfSatisfying(GameException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(GameErrorCode.INVALID_AGE_GROUP_FORMAT));
    }

    private AgeGroup ageGroupAt(int age) {
        return AgeGroup.fromBirthDate(EXERCISE_DATE.minusYears(age), EXERCISE_DATE);
    }
}
