package umc.cockple.demo.domain.game.presentation.dto;

import java.time.LocalDateTime;
import java.util.List;

public class GameCompletedGameDTO {

    public record Response(
            List<CompletedGameInfo> games,
            String nextCursor,
            boolean hasNext
    ) {
    }

    public record CompletedGameInfo(
            Long gameId,
            int courtNo,
            LocalDateTime completedAt,
            int durationMin,
            List<PlayerInfo> players
    ) {
    }

    public record PlayerInfo(
            Long gameBoardMemberId,
            String name,
            String level,
            int playerOrder
    ) {
    }
}
