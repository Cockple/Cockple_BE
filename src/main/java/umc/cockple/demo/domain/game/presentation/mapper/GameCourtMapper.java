package umc.cockple.demo.domain.game.presentation.mapper;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.game.presentation.dto.GameCourtDTO;
import umc.cockple.demo.domain.game.service.command.model.GameCourtManageCommand;
import umc.cockple.demo.domain.game.service.command.result.GameCourtManageResult;

import java.util.List;

/**
 * 게임판 코트: 요청 DTO ↔ 내부 command/result ↔ 응답 DTO 변환.
 * 서비스가 presentation 타입을 모르도록 경계에서만 변환한다.
 */
@Component
public class GameCourtMapper {

    public GameCourtManageCommand toManageCommand(Long gameBoardId, GameCourtDTO.Request request) {
        List<GameCourtDTO.CourtRequest> items =
                request.courts() == null ? List.of() : request.courts();
        List<GameCourtManageCommand.CourtCommand> courts = items.stream()
                .map(item -> new GameCourtManageCommand.CourtCommand(item.courtId(), item.courtName()))
                .toList();
        return new GameCourtManageCommand(gameBoardId, courts);
    }

    public GameCourtDTO.Response toResponse(GameCourtManageResult result) {
        List<GameCourtDTO.CourtInfo> courts = result.courts().stream()
                .map(court -> new GameCourtDTO.CourtInfo(
                        court.courtId(), court.courtNo(), court.courtName()))
                .toList();
        return new GameCourtDTO.Response(result.gameBoardId(), courts);
    }
}
