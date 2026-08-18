package umc.cockple.demo.domain.game.enums;

import lombok.Getter;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;

import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;

@Getter
public enum AgeGroup {

    TEENS("10대"),
    TWENTIES("20대"),
    THIRTIES("30대"),
    FORTIES("40대"),
    FIFTIES("50대"),
    SIXTIES("60대"),
    SEVENTIES("70대");

    private final String koreanName;

    AgeGroup(String koreanName) {
        this.koreanName = koreanName;
    }

    public static AgeGroup fromKorean(String koreanName) {
        if (koreanName != null) {
            String normalized = koreanName.trim();
            return Arrays.stream(values())
                    .filter(ageGroup -> ageGroup.koreanName.equals(normalized))
                    .findFirst()
                    .orElseThrow(AgeGroup::invalidAgeGroup);
        }
        throw invalidAgeGroup();
    }

    /**
     * 운동일 기준 만 나이를 10년 단위 연령대로 변환한다.
     * 지원 범위(10~79세) 밖이거나 생년월일/운동일이 없으면 null을 반환한다.
     */
    public static AgeGroup fromBirthDate(LocalDate birthDate, LocalDate exerciseDate) {
        if (birthDate == null || exerciseDate == null || birthDate.isAfter(exerciseDate)) {
            return null;
        }

        int age = Period.between(birthDate, exerciseDate).getYears();
        return switch (age / 10) {
            case 1 -> TEENS;
            case 2 -> TWENTIES;
            case 3 -> THIRTIES;
            case 4 -> FORTIES;
            case 5 -> FIFTIES;
            case 6 -> SIXTIES;
            case 7 -> SEVENTIES;
            default -> null;
        };
    }

    private static GameException invalidAgeGroup() {
        return new GameException(GameErrorCode.INVALID_AGE_GROUP_FORMAT);
    }
}
