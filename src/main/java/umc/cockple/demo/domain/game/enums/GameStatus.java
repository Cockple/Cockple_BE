package umc.cockple.demo.domain.game.enums;

import lombok.Getter;

@Getter
public enum GameStatus {

    WAITING("대기"),
    PLAYING("진행중"),
    COMPLETED("완료");

    private final String koreanName;

    GameStatus(String koreanName) {
        this.koreanName = koreanName;
    }
}
