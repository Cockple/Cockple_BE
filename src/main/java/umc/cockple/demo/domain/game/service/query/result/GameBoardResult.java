package umc.cockple.demo.domain.game.service.query.result;

import umc.cockple.demo.domain.game.enums.CourtStatus;
import umc.cockple.demo.global.enums.Level;

import java.time.LocalDateTime;
import java.util.List;

public record GameBoardResult(
        boolean isGameHost,
        int courtCount,
        List<CourtView> courts,
        List<WaitingView> waitings
) {
    public record CourtView(
            Long courtId,
            int courtNo,
            String courtName,
            CourtStatus status,
            GameView game
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
            String profileImageUrl,
            Level level,
            int playerOrder
    ) {
    }
}
