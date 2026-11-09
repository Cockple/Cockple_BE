package umc.cockple.demo.domain.game.service.support.validator;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;

@Component
public class GameValidator {

    public void validateBelongsToBoard(Game game, Long gameBoardId) {
        if (!game.getGameBoard().getId().equals(gameBoardId)) {
            throw new GameException(GameErrorCode.GAME_NOT_FOUND);
        }
    }

    public void validateWaiting(Game game) {
        if (game.getStatus() != GameStatus.WAITING) {
            throw new GameException(GameErrorCode.GAME_NOT_WAITING);
        }
    }

    public void validatePlaying(Game game) {
        if (game.getStatus() != GameStatus.PLAYING) {
            throw new GameException(GameErrorCode.GAME_NOT_PLAYING);
        }
    }

    public void validateNotCompleted(Game game) {
        if (game.getStatus() == GameStatus.COMPLETED) {
            throw new GameException(GameErrorCode.GAME_ALREADY_COMPLETED);
        }
    }
}
