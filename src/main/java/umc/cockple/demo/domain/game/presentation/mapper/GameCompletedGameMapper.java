package umc.cockple.demo.domain.game.presentation.mapper;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.game.presentation.dto.GameCompletedGameDTO;
import umc.cockple.demo.domain.game.service.query.result.GameCompletedGameResult;

@Component
public class GameCompletedGameMapper {

    public GameCompletedGameDTO.Response toResponse(GameCompletedGameResult result) {
        return new GameCompletedGameDTO.Response(
                result.games().stream().map(this::toGameInfo).toList(),
                result.nextCursor(),
                result.hasNext());
    }

    private GameCompletedGameDTO.CompletedGameInfo toGameInfo(GameCompletedGameResult.CompletedGameView game) {
        return new GameCompletedGameDTO.CompletedGameInfo(
                game.gameId(),
                game.courtNo(),
                game.completedAt(),
                game.durationMin(),
                game.players().stream().map(this::toPlayerInfo).toList());
    }

    private GameCompletedGameDTO.PlayerInfo toPlayerInfo(GameCompletedGameResult.PlayerView player) {
        return new GameCompletedGameDTO.PlayerInfo(
                player.gameBoardMemberId(),
                player.name(),
                player.level().getKoreanName(),
                player.playerOrder());
    }
}
