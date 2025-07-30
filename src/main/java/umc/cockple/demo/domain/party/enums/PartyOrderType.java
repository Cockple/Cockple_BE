package umc.cockple.demo.domain.party.enums;

import lombok.Getter;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;

import java.util.Arrays;

public enum PartyOrderType {

    LATEST("최신순"),
    OLDEST("오래된 순"),
    EXERCISE_COUNT("운동 많은 순");

    @Getter
    private final String koreanName;

    PartyOrderType(String koreanName) {
        this.koreanName = koreanName;
    }

    public static PartyOrderType fromKorean(String korean) {
        return Arrays.stream(values())
                .filter(sortType -> sortType.koreanName.equals(korean.trim()))
                .findFirst()
                .orElseThrow(() -> new PartyException(PartyErrorCode.INVALID_ORDER_TYPE));
    }

}
