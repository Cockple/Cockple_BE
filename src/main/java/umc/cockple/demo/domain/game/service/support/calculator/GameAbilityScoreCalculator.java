package umc.cockple.demo.domain.game.service.support.calculator;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.enums.AgeGroup;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

@Component
public class GameAbilityScoreCalculator {

    public int calculate(GameBoardMember member) {
        int baseScore = baseScore(member.getLevel());
        if (member.getGender() == Gender.FEMALE) {
            return baseScore - 10;
        }
        return baseScore + maleAgeBonus(member.getAgeGroup());
    }

    private int baseScore(Level level) {
        return switch (level) {
            case EXPERT -> 100;
            case SEMI_EXPERT -> 90;
            case A -> 80;
            case B -> 70;
            case C -> 60;
            case D -> 50;
            case BEGINNER -> 40;
            case NOVICE -> 30;
            case NONE -> throw new IllegalArgumentException(
                    "급수없음 선수는 능력치 점수 계산 대상이 아닙니다.");
        };
    }

    private int maleAgeBonus(AgeGroup ageGroup) {
        if (ageGroup == null) {
            return 4;
        }
        return switch (ageGroup) {
            case TEENS, TWENTIES -> 6;
            case THIRTIES -> 4;
            case FORTIES -> 2;
            case FIFTIES, SIXTIES, SEVENTIES -> 0;
        };
    }
}
