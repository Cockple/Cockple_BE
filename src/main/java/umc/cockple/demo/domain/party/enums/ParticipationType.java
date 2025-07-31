package umc.cockple.demo.domain.party.enums;

import lombok.Getter;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;

import java.util.Arrays;

public enum ParticipationType {

    SINGLE("단식"),
    WOMEN_DOUBLES("여복"),
    MEN_DOUBLES("남복"),
    MIX_DOUBLES("혼복");

    @Getter
    private final String koreanName;

    ParticipationType(String koreanName) {
        this.koreanName = koreanName;
    }

    public static ParticipationType fromKorean(String korean) {
        return Arrays.stream(values())
                .filter(sortType -> sortType.koreanName.equals(korean.trim()))
                .findFirst()
                .orElseThrow(() -> new PartyException(PartyErrorCode.INVALID_PARTY_TYPE));
    }
}
