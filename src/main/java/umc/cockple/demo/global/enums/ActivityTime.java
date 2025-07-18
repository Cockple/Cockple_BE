package umc.cockple.demo.global.enums;

import lombok.Getter;

public enum ActivityTime {

    MORNING("오전"),
    AFTERNOON("오후"),
    ALWAYS("상시");

    @Getter
    private final String koreanName;

    ActivityTime(String koreanName) {
        this.koreanName = koreanName;
    }

}
