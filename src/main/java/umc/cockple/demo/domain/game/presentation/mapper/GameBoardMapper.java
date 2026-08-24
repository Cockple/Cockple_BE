package umc.cockple.demo.domain.game.presentation.mapper;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.game.presentation.dto.GameBoardDTO;
import umc.cockple.demo.domain.game.service.query.result.GameBoardResult;

@Component
public class GameBoardMapper {

    public GameBoardDTO.Response toResponse(GameBoardResult result) {
        return new GameBoardDTO.Response(
                result.isGameHost(),
                result.courtCount(),
                result.courts().stream().map(this::toCourtInfo).toList(),
                result.waitings().stream().map(this::toWaitingInfo).toList());
    }

    private GameBoardDTO.CourtInfo toCourtInfo(GameBoardResult.CourtView court) {
        return new GameBoardDTO.CourtInfo(
                court.courtId(),
                court.courtNo(),
                court.courtName(),
                court.status(),
                court.game() == null ? null : toGameInfo(court.game()));
    }

    private GameBoardDTO.GameInfo toGameInfo(GameBoardResult.GameView game) {
        return new GameBoardDTO.GameInfo(
                game.gameId(),
                game.startedAt(),
                game.players().stream().map(this::toPlayerInfo).toList());
    }

    private GameBoardDTO.WaitingInfo toWaitingInfo(GameBoardResult.WaitingView waiting) {
        return new GameBoardDTO.WaitingInfo(
                waiting.gameId(),
                waiting.waitingOrder(),
                waiting.players().stream().map(this::toPlayerInfo).toList());
    }

    private GameBoardDTO.PlayerInfo toPlayerInfo(GameBoardResult.PlayerView player) {
        return new GameBoardDTO.PlayerInfo(
                player.gameBoardMemberId(),
                player.name(),
                player.level().getKoreanName(),
                player.playerOrder());
    }
}
