package umc.cockple.demo.domain.game.service.query.result;

import umc.cockple.demo.global.enums.Level;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 완료된 게임 조회 결과
 */
public record GameCompletedGameResult(
        List<CompletedGameView> games,
        String nextCursor,   // 다음 페이지 커서(없으면 null)
        boolean hasNext
) {
    public record CompletedGameView(
            Long gameId,
            int courtNo,
            LocalDateTime completedAt,
            int durationMin,
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
