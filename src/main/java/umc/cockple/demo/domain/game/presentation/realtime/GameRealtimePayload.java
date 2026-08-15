package umc.cockple.demo.domain.game.presentation.realtime;

public record GameRealtimePayload(
        Long gameBoardId,      // 공통
        Long courtId,          // 게임이 현재 올라간 원본 코트 ID
        Integer targetCourtNo  // 이동할 목적지 코트 번호
) {
}
