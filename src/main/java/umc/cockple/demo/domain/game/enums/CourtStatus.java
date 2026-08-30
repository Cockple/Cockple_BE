package umc.cockple.demo.domain.game.enums;

/**
 * 코트의 조회 시점 상태(파생값, 저장하지 않음).
 * 해당 코트에 PLAYING 게임이 올라가 있으면 PLAYING, 아니면 EMPTY.
 */
public enum CourtStatus {
    EMPTY,
    PLAYING
}
