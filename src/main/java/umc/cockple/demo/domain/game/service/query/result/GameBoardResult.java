package umc.cockple.demo.domain.game.service.query.result;

import umc.cockple.demo.domain.game.enums.CourtStatus;
import umc.cockple.demo.global.enums.Level;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 코트 보드 조회(#2) 내부 result 모델. (명단 제외한 코트/대기열)
 */
public record GameBoardResult(
        int courtCount,
        List<CourtView> courts,
        List<WaitingView> waitings
) {
    public record CourtView(
            Long courtId,
            int courtNo,
            String courtName,
            CourtStatus status,
            GameView game // EMPTY 이면 null
    ) {
    }

    public record GameView(
            Long gameId,
            LocalDateTime startedAt,
            List<PlayerView> players
    ) {
    }

    public record WaitingView(
            Long gameId,
            int waitingOrder,
            List<PlayerView> players
    ) {
    }

    public record PlayerView(
            Long gameBoardMemberId,
            String name,
            Level level,
            int playerOrder
    ) {
    }
}
