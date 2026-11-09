package umc.cockple.demo.domain.game.service.support.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.domain.service.GameBoardMemberAvailabilityPolicy;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GamePlayerValidator {

    private final GameBoardMemberAvailabilityPolicy availabilityPolicy;

    public void validateMembers(List<Long> requestedMemberIds, Map<Long, GameBoardMember> membersById) {
        if (membersById.size() != requestedMemberIds.size()) {
            throw new GameException(GameErrorCode.GAME_BOARD_MEMBER_NOT_FOUND);
        }
        if (membersById.values().stream()
                .anyMatch(member -> !Boolean.TRUE.equals(member.getParticipating()))) {
            throw new GameException(GameErrorCode.INACTIVE_GAME_PLAYER);
        }
    }

    public void validateWaitingAvailability(Map<Long, GameBoardMember> membersById, List<Game> activeGames) {
        if (availabilityPolicy.hasWaitingConflict(List.copyOf(membersById.values()), activeGames)) {
            throw new GameException(GameErrorCode.UNAVAILABLE_GAME_PLAYER);
        }
    }
}
