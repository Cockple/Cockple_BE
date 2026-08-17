package umc.cockple.demo.domain.game.presentation.realtime;

import java.util.List;

public record GameRealtimePayload(
        Long gameBoardId,               // 공통
        Long courtId,                   // MOVE_COURT: 원본 코트 ID / START_GAME: 목적지 코트 ID
        Integer targetCourtNo,          // MOVE_COURT: 이동할 목적지 코트 번호
        Long gameId,                    // START_GAME: 시작할 대기 게임 ID
        List<Long> gameBoardMemberIds   // CREATE_GAME: 게임에 넣을 멤버(순서 = 플레이어 순서)
) {
}
