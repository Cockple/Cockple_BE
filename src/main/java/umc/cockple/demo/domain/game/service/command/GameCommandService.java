package umc.cockple.demo.domain.game.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.game.domain.Court;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.repository.CourtRepository;
import umc.cockple.demo.domain.game.repository.GameRepository;
import umc.cockple.demo.domain.game.service.command.model.GameStartCommand;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GameCommandService {

    private final GameBoardReader gameBoardReader;
    private final GameRepository gameRepository;
    private final CourtRepository courtRepository;

    /**
     * @param memberId 요청자(추후 게임판 관리 권한 검증에 사용 예정)
     */
    public void startGame(Long memberId, GameStartCommand command) {
        GameBoard gameBoard = gameBoardReader.read(command.gameBoardId());

        Game game = gameRepository.findById(command.gameId())
                .orElseThrow(() -> new GameException(GameErrorCode.GAME_NOT_FOUND));
        if (!game.getGameBoard().getId().equals(gameBoard.getId())) {
            throw new GameException(GameErrorCode.GAME_NOT_FOUND);
        }
        if (game.getStatus() != GameStatus.WAITING) {
            throw new GameException(GameErrorCode.GAME_NOT_WAITING);
        }

        Court court = courtRepository.findByIdAndGameBoardId(command.courtId(), gameBoard.getId())
                .orElseThrow(() -> new GameException(GameErrorCode.COURT_NOT_FOUND));
        if (gameRepository.findByCourtIdAndStatus(court.getId(), GameStatus.PLAYING).isPresent()) {
            throw new GameException(GameErrorCode.COURT_ALREADY_IN_USE);
        }

        game.start(court, LocalDateTime.now());
        resequenceWaitingQueue(gameBoard.getId());

        log.info("게임 시작 - gameBoardId: {}, gameId: {}, courtId: {}",
                gameBoard.getId(), game.getId(), court.getId());
    }

    /**
     * 대기열에 남은 게임들의 순서를 현재 순서 기준으로 1부터 다시 매긴다. (빈 순서 제거)
     */
    private void resequenceWaitingQueue(Long gameBoardId) {
        List<Game> waitingGames = gameRepository
                .findByGameBoardIdAndStatusOrderByWaitingOrderAsc(gameBoardId, GameStatus.WAITING);
        int order = 1;
        for (Game waitingGame : waitingGames) {
            waitingGame.changeWaitingOrder(order++);
        }
    }
}
