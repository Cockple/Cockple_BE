package umc.cockple.demo.global.enums;

import lombok.Getter;

public enum MyPartyExerciseOrderType {

    LATEST("최신순"),
    POPULARITY("인기순");

    @Getter
    private final String koreanName;

    MyPartyExerciseOrderType(String koreanName) {
        this.koreanName = koreanName;
    }
}
