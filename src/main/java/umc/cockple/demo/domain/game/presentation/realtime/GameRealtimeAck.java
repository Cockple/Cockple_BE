package umc.cockple.demo.domain.game.presentation.realtime;

import umc.cockple.demo.domain.game.presentation.dto.GameBoardDTO;

import java.util.List;

public final class GameRealtimeAck {

    private GameRealtimeAck() {
    }

    public record SubscriptionAck(Long gameBoardId) {
    }

    public record GameCreatedAck(Long gameId, GameBoardDTO.Response board) {
    }

    public record GameDeletedAck(Long gameId, List<DeletedPlayer> players, GameBoardDTO.Response board) {
    }

    public record DeletedPlayer(Long gameBoardMemberId, String name, String level, int playerOrder) {
    }
}
