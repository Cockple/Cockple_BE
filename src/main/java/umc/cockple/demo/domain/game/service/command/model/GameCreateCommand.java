package umc.cockple.demo.domain.game.service.command.model;

import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 게임 대기 생성 내부 command 모델
 * gameBoardMemberIds 의 배열 순서가 곧 게임 내 플레이어 순서(playerOrder)가 된다.
 */
public record GameCreateCommand(
        Long gameBoardId,
        List<Long> gameBoardMemberIds
) {
    private static final int MAX_PLAYERS = 4;

    public GameCreateCommand {
        if (gameBoardId == null) {
            throw new GameException(GameErrorCode.GAME_BOARD_ID_REQUIRED);
        }
        validatePlayerCount(gameBoardMemberIds);
        validateNoDuplicatePlayer(gameBoardMemberIds);
    }

    private static void validatePlayerCount(List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > MAX_PLAYERS) {
            throw new GameException(GameErrorCode.INVALID_GAME_PLAYER_COUNT);
        }
    }

    private static void validateNoDuplicatePlayer(List<Long> ids) {
        Set<Long> seen = new HashSet<>();
        boolean hasDuplicate = ids.stream().anyMatch(id -> !seen.add(id));
        if (hasDuplicate) {
            throw new GameException(GameErrorCode.DUPLICATE_GAME_PLAYER);
        }
    }
}
