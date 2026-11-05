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

    /**
     * 랜덤 매칭 정책
     * <p>
     * 불참 선수와 활성 게임(WAITING, PLAYING)에 속한 선수를 모두 제외한다.
     * 수동 편성({@link #hasWaitingConflict})은 PLAYING 선수를 허용하므로 두 정책은 의도적으로 다르다.
     */
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
     * <p>
     * 대기(WAITING) 게임에 이미 편성된 선수만 충돌로 본다.
     * 랜덤 매칭({@link #filterAvailable})과 달리 PLAYING 선수는 허용한다.
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
