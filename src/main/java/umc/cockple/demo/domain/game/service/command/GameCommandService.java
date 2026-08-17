package umc.cockple.demo.domain.game.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.game.domain.Court;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.domain.GamePlayer;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.repository.CourtRepository;
import umc.cockple.demo.domain.game.repository.GameBoardMemberRepository;
import umc.cockple.demo.domain.game.repository.GameRepository;
import umc.cockple.demo.domain.game.service.command.model.GameCompleteCommand;
import umc.cockple.demo.domain.game.service.command.model.GameCreateCommand;
import umc.cockple.demo.domain.game.service.command.model.GameStartCommand;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GameCommandService {

    private final GameBoardReader gameBoardReader;
    private final GameRepository gameRepository;
    private final CourtRepository courtRepository;
    private final GameBoardMemberRepository gameBoardMemberRepository;

    /**
     * 게임 대기 생성
     *
     * @param memberId 요청자(추후 게임판 관리 권한 검증에 사용 예정)
     * @return 생성된 게임 ID
     */
    public Long createGame(Long memberId, GameCreateCommand command) {
        GameBoard gameBoard = gameBoardReader.read(command.gameBoardId());

        Map<Long, GameBoardMember> membersById = gameBoardMemberRepository
                .findByGameBoardIdAndIdIn(gameBoard.getId(), command.gameBoardMemberIds()).stream()
                .collect(Collectors.toMap(GameBoardMember::getId, Function.identity()));
        if (membersById.size() != command.gameBoardMemberIds().size()) {
            throw new GameException(GameErrorCode.GAME_BOARD_MEMBER_NOT_FOUND);
        }

        int nextWaitingOrder = (int) gameRepository
                .countByGameBoardIdAndStatus(gameBoard.getId(), GameStatus.WAITING) + 1;
        Game game = Game.createWaiting(gameBoard, nextWaitingOrder);

        int playerOrder = 0;
        for (Long gameBoardMemberId : command.gameBoardMemberIds()) {
            game.addPlayer(GamePlayer.create(membersById.get(gameBoardMemberId), playerOrder++));
        }

        Game savedGame = gameRepository.save(game);
        log.info("게임 대기 생성 - gameBoardId: {}, gameId: {}, 인원: {}",
                gameBoard.getId(), savedGame.getId(), command.gameBoardMemberIds().size());
        return savedGame.getId();
    }

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
     * 게임 완료 (#5)
     *
     * @param memberId 요청자
     * @return 완료된 게임 ID
     */
    public Long completeGame(Long memberId, GameCompleteCommand command) {
        GameBoard gameBoard = gameBoardReader.read(command.gameBoardId());

        Game game = gameRepository.findById(command.gameId())
                .orElseThrow(() -> new GameException(GameErrorCode.GAME_NOT_FOUND));
        if (!game.getGameBoard().getId().equals(gameBoard.getId())) {
            throw new GameException(GameErrorCode.GAME_NOT_FOUND);
        }
        if (game.getStatus() != GameStatus.PLAYING) {
            throw new GameException(GameErrorCode.GAME_NOT_PLAYING);
        }

        game.complete(LocalDateTime.now());
        game.getPlayers().forEach(player -> player.getGameBoardMember().increaseGameCount());

        log.info("게임 완료 - gameBoardId: {}, gameId: {}, 인원: {}",
                gameBoard.getId(), game.getId(), game.getPlayers().size());
        return game.getId();
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
