package umc.cockple.demo.domain.game.service.support.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.game.domain.Court;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.repository.GameRepository;
import umc.cockple.demo.domain.game.service.command.model.GameCourtManageCommand;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class GameCourtValidator {

    private final GameRepository gameRepository;

    public void validateRequestedCourtsBelongToBoard(
            List<GameCourtManageCommand.CourtCommand> requestedCourts,
            Map<Long, Court> existingCourtsById) {
        boolean hasForeignCourt = requestedCourts.stream()
                .map(GameCourtManageCommand.CourtCommand::courtId)
                .filter(Objects::nonNull)
                .anyMatch(courtId -> !existingCourtsById.containsKey(courtId));
        if (hasForeignCourt) {
            throw new GameException(GameErrorCode.COURT_NOT_FOUND);
        }
    }

    /**
     * 진행 중(PLAYING)인 게임이 배정된 코트는 삭제할 수 없다.
     * 삭제하면 코트 위에서 진행되던 게임이 유실되므로 사전에 차단한다.
     */
    public void validateNoActiveGameOnCourts(List<Court> removedCourts) {
        List<Long> removedCourtIds = removedCourts.stream()
                .map(Court::getId)
                .toList();
        if (gameRepository.existsByCourtIdInAndStatus(removedCourtIds, GameStatus.PLAYING)) {
            throw new GameException(GameErrorCode.COURT_HAS_ACTIVE_GAME);
        }
    }

}
