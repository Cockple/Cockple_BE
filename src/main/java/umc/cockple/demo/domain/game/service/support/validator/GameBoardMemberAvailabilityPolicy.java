package umc.cockple.demo.domain.game.service.support.validator;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.domain.GamePlayer;
import umc.cockple.demo.domain.game.enums.GameStatus;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class GameBoardMemberAvailabilityPolicy {

    private static final long PLAYING_COOLDOWN_MINUTES = 10;

    public List<GameBoardMember> filterAvailable(
            List<GameBoardMember> members,
            List<Game> activeGames,
            LocalDateTime now) {
        Set<Long> blockedMemberIds = blockedMemberIds(activeGames, now);
        return members.stream()
                .filter(member -> Boolean.TRUE.equals(member.getParticipating()))
                .filter(member -> !blockedMemberIds.contains(member.getId()))
                .toList();
    }

    public boolean hasBlockedMember(
            List<GameBoardMember> members,
            List<Game> activeGames,
            LocalDateTime now) {
        Set<Long> blockedMemberIds = blockedMemberIds(activeGames, now);
        return members.stream().anyMatch(member -> blockedMemberIds.contains(member.getId()));
    }

    private Set<Long> blockedMemberIds(List<Game> activeGames, LocalDateTime now) {
        Set<Long> blockedMemberIds = new HashSet<>();
        for (Game game : activeGames) {
            if (!blocksSelection(game, now)) {
                continue;
            }
            game.getPlayers().stream()
                    .map(GamePlayer::getGameBoardMember)
                    .map(GameBoardMember::getId)
                    .forEach(blockedMemberIds::add);
        }
        return blockedMemberIds;
    }

    private boolean blocksSelection(Game game, LocalDateTime now) {
        if (game.getStatus() == GameStatus.WAITING) {
            return true;
        }
        if (game.getStatus() != GameStatus.PLAYING) {
            return false;
        }
        LocalDateTime startedAt = game.getStartedAt();
        return startedAt == null || startedAt.isAfter(now.minusMinutes(PLAYING_COOLDOWN_MINUTES));
    }
}
