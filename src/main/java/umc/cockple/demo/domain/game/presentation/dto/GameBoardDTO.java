package umc.cockple.demo.domain.game.presentation.dto;

import umc.cockple.demo.domain.game.enums.CourtStatus;

import java.time.LocalDateTime;
import java.util.List;

public class GameBoardDTO {

    public record Response(
            int courtCount,
            List<CourtInfo> courts,
            List<WaitingInfo> waitings
    ) {
    }

    public record CourtInfo(
            Long courtId,
            int courtNo,
            String courtName,
            CourtStatus status,
            GameInfo game // status 가 EMPTY 이면 null
    ) {
    }

    public record GameInfo(
            Long gameId,
            LocalDateTime startedAt,
            List<PlayerInfo> players
    ) {
    }

    public record WaitingInfo(
            Long gameId,
            int waitingOrder,
            List<PlayerInfo> players
    ) {
    }

    public record PlayerInfo(
            Long gameBoardMemberId,
            String name,
            String level, // 급수 한글 표기 (예: "준자강", "A조")
            int playerOrder
    ) {
    }
}
