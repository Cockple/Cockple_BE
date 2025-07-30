package umc.cockple.demo.domain.exercise.enums;

import lombok.Getter;

public enum MyExerciseFilterType {

    ALL("전체"),
    UPCOMING("참여 예정"),
    COMPLETED("참여 완료");

    @Getter
    private final String koreanName;

    MyExerciseFilterType(String koreanName) {
        this.koreanName = koreanName;
    }
}
