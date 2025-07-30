package umc.cockple.demo.domain.exercise.enums;

import lombok.Getter;

public enum MyExerciseOrderType {

    LATEST("최신순"),
    OLDEST("오래된순");

    @Getter
    private final String koreanName;

    MyExerciseOrderType(String koreanName) {
        this.koreanName = koreanName;
    }
}
