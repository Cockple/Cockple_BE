package umc.cockple.demo.global.enums;

import lombok.Getter;

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

}
