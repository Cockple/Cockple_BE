package umc.cockple.demo.domain.game.enums;

import lombok.Getter;

import java.util.List;

@Getter
public enum GameStatus {

    WAITING("대기"),
    PLAYING("진행중"),
    COMPLETED("완료");

    public static final List<GameStatus> ACTIVE_STATUSES = List.of(WAITING, PLAYING);

    private final String koreanName;

    GameStatus(String koreanName) {
        this.koreanName = koreanName;
    }

    public boolean isActive() {
        return ACTIVE_STATUSES.contains(this);
    }
}
