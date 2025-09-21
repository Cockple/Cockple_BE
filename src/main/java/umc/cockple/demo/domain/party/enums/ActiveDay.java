package umc.cockple.demo.domain.party.enums;

import lombok.Getter;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;

import java.util.Arrays;

public enum ActiveDay {

    SUNDAY("일"),
    MONDAY("월"),
    TUESDAY("화"),
    WEDNESDAY("수"),
    THURSDAY("목"),
    FRIDAY("금"),
    SATURDAY("토");

    @Getter
    private final String koreanName;

    ActiveDay(String koreanName) {
        this.koreanName = koreanName;
    }

    public static ActiveDay fromKorean(String korean){
        return Arrays.stream(values())
                .filter(day -> day.koreanName.equals(korean.trim()))
                .findFirst()
                .orElseThrow(() -> new PartyException(PartyErrorCode.INVALID_ACTIVITY_DAY));
    }
}
