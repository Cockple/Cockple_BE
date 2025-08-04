package umc.cockple.demo.domain.chat.enums;

import lombok.Getter;

public enum Direction {
    DESC("내림차순"),
    ACS("오름차순");

    @Getter
    private final String description;

    Direction(String description) {
        this.description = description;
    }

}
