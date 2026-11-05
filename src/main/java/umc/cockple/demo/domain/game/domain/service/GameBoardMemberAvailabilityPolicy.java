package umc.cockple.demo.domain.game.domain.service;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.domain.GamePlayer;
import umc.cockple.demo.domain.game.enums.GameStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class GameBoardMemberAvailabilityPolicy {

    public List<GameBoardMember> filterAvailable(
            List<GameBoardMember> members,
            List<Game> activeGames) {
        Set<Long> blockedMemberIds = blockedMemberIds(activeGames);
        return members.stream()
                .filter(member -> Boolean.TRUE.equals(member.getParticipating()))
                .filter(member -> !blockedMemberIds.contains(member.getId()))
                .toList();
    }

    /**
     * 수동 게임 편성 정책
     */
    public boolean hasWaitingConflict(
            List<GameBoardMember> members,
            List<Game> activeGames) {
        Set<Long> waitingMemberIds = new HashSet<>();
        for (Game game : activeGames) {
            if (game.getStatus() != GameStatus.WAITING) {
                continue;
            }
            game.getPlayers().stream()
                    .map(GamePlayer::getGameBoardMember)
                    .map(GameBoardMember::getId)
                    .forEach(waitingMemberIds::add);
        }
        return members.stream().anyMatch(member -> waitingMemberIds.contains(member.getId()));
    }

    private Set<Long> blockedMemberIds(List<Game> activeGames) {
        Set<Long> blockedMemberIds = new HashSet<>();
        for (Game game : activeGames) {
            if (!blocksSelection(game)) {
                continue;
            }
            game.getPlayers().stream()
                    .map(GamePlayer::getGameBoardMember)
                    .map(GameBoardMember::getId)
                    .forEach(blockedMemberIds::add);
        }
        return blockedMemberIds;
    }

    private boolean blocksSelection(Game game) {
        return game.getStatus().isActive();
    }
}
