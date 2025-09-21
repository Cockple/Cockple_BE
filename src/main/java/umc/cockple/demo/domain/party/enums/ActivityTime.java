package umc.cockple.demo.domain.party.enums;

import lombok.Getter;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;

import java.util.Arrays;

public enum ActivityTime {

    MORNING("오전"),
    AFTERNOON("오후"),
    ALWAYS("상시");

    @Getter
    private final String koreanName;

    ActivityTime(String koreanName) {
        this.koreanName = koreanName;
    }

    public static ActivityTime fromKorean(String korean){
        return Arrays.stream(values())
                .filter(time -> time.koreanName.equals(korean.trim()))
                .findFirst()
                .orElseThrow(() -> new PartyException(PartyErrorCode.INVALID_ACTIVITY_TIME));
    }

}
