package umc.cockple.demo.domain.game.service.support.reader;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.repository.GameBoardRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GameBoardReader {

    private final GameBoardRepository gameBoardRepository;

    public GameBoard read(Long gameBoardId) {
        return gameBoardRepository.findById(gameBoardId)
                .orElseThrow(() -> new GameException(GameErrorCode.GAME_BOARD_NOT_FOUND));
    }

    @Transactional
    public GameBoard readForUpdate(Long gameBoardId) {
        return gameBoardRepository.findByIdForUpdate(gameBoardId)
                .orElseThrow(() -> new GameException(GameErrorCode.GAME_BOARD_NOT_FOUND));
    }
}
