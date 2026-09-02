package umc.cockple.demo.domain.game.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.game.domain.Court;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.domain.GamePlayer;
import umc.cockple.demo.domain.game.domain.service.GameBoardMemberAvailabilityPolicy;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.events.GameBoardMembersChangedEvent;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.repository.CourtRepository;
import umc.cockple.demo.domain.game.repository.GameBoardMemberRepository;
import umc.cockple.demo.domain.game.repository.GameRepository;
import umc.cockple.demo.domain.game.service.command.model.GameCompleteCommand;
import umc.cockple.demo.domain.game.service.command.model.GameCreateCommand;
import umc.cockple.demo.domain.game.service.command.model.GameDeleteCommand;
import umc.cockple.demo.domain.game.service.command.model.GameStartCommand;
import umc.cockple.demo.domain.game.service.command.model.GameToWaitingCommand;
import umc.cockple.demo.domain.game.service.command.result.GameDeleteResult;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;
import umc.cockple.demo.domain.game.service.support.validator.GameBoardAccessValidator;

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

    private static final List<GameStatus> ACTIVE_STATUSES =
            List.of(GameStatus.WAITING, GameStatus.PLAYING);

    private final GameBoardReader gameBoardReader;
    private final GameRepository gameRepository;
    private final CourtRepository courtRepository;
    private final GameBoardMemberRepository gameBoardMemberRepository;
    private final GameBoardAccessValidator gameBoardAccessValidator;
    private final GameBoardMemberAvailabilityPolicy availabilityPolicy;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 게임 대기 생성
     *
     * @param memberId 요청자
     * @return 생성된 게임 ID
     */
    public Long createGame(Long memberId, GameCreateCommand command) {
        GameBoard gameBoard = gameBoardReader.readForUpdate(command.gameBoardId());
        gameBoardAccessValidator.validateGameHost(gameBoard.getId(), memberId);

        Map<Long, GameBoardMember> membersById = gameBoardMemberRepository
                .findByGameBoardIdAndIdIn(gameBoard.getId(), command.gameBoardMemberIds()).stream()
                .collect(Collectors.toMap(GameBoardMember::getId, Function.identity()));
        if (membersById.size() != command.gameBoardMemberIds().size()) {
            throw new GameException(GameErrorCode.GAME_BOARD_MEMBER_NOT_FOUND);
        }
        if (membersById.values().stream()
                .anyMatch(gameBoardMember -> !Boolean.TRUE.equals(gameBoardMember.getParticipating()))) {
            throw new GameException(GameErrorCode.INACTIVE_GAME_PLAYER);
        }
        List<Game> activeGames = gameRepository.findByGameBoardIdAndStatusInWithPlayers(
                gameBoard.getId(), ACTIVE_STATUSES);

        if (availabilityPolicy.hasWaitingConflict(
                List.copyOf(membersById.values()), activeGames)) {
            throw new GameException(GameErrorCode.UNAVAILABLE_GAME_PLAYER);
        }

        int nextWaitingOrder = (int) gameRepository
                .countByGameBoardIdAndStatus(gameBoard.getId(), GameStatus.WAITING) + 1;
        Game game = Game.createWaiting(gameBoard, nextWaitingOrder);

        int playerOrder = 0;
        for (Long gameBoardMemberId : command.gameBoardMemberIds()) {
            game.addPlayer(GamePlayer.create(membersById.get(gameBoardMemberId), playerOrder++));
        }

        Game savedGame = gameRepository.save(game);
        publishMembersChanged(gameBoard.getId(), memberId);
        log.info("게임 대기 생성 - gameBoardId: {}, gameId: {}, 인원: {}",
                gameBoard.getId(), savedGame.getId(), command.gameBoardMemberIds().size());
        return savedGame.getId();
    }

    /**
     * @param memberId 요청자
     */
    public void startGame(Long memberId, GameStartCommand command) {
        GameBoard gameBoard = gameBoardReader.read(command.gameBoardId());
        gameBoardAccessValidator.validateGameHost(gameBoard.getId(), memberId);

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
        publishMembersChanged(gameBoard.getId(), memberId);

        log.info("게임 시작 - gameBoardId: {}, gameId: {}, courtId: {}",
                gameBoard.getId(), game.getId(), court.getId());
    }

    /**
     * 게임 완료
     *
     * @param memberId 요청자
     */
    public void completeGame(Long memberId, GameCompleteCommand command) {
        GameBoard gameBoard = gameBoardReader.read(command.gameBoardId());
        gameBoardAccessValidator.validateGameHost(gameBoard.getId(), memberId);

        Game game = gameRepository.findById(command.gameId())
                .orElseThrow(() -> new GameException(GameErrorCode.GAME_NOT_FOUND));
        if (!game.getGameBoard().getId().equals(gameBoard.getId())) {
            throw new GameException(GameErrorCode.GAME_NOT_FOUND);
        }
        if (game.getStatus() != GameStatus.PLAYING) {
            throw new GameException(GameErrorCode.GAME_NOT_PLAYING);
        }

        completeInternal(game);
        publishMembersChanged(gameBoard.getId(), memberId);

        log.info("게임 완료 - gameBoardId: {}, gameId: {}", gameBoard.getId(), game.getId());
    }

    private void completeInternal(Game game) {
        game.complete(LocalDateTime.now());
        game.getPlayers().forEach(player -> player.getGameBoardMember().increaseGameCount());
    }

    /**
     * 게임 취소/대기 삭제
     *
     * @param memberId 요청자
     * @return 삭제된 게임 ID + (restore 시) 플레이어 목록
     */
    public GameDeleteResult deleteGame(Long memberId, GameDeleteCommand command) {
        GameBoard gameBoard = gameBoardReader.read(command.gameBoardId());
        gameBoardAccessValidator.validateGameHost(gameBoard.getId(), memberId);

        Game game = gameRepository.findById(command.gameId())
                .orElseThrow(() -> new GameException(GameErrorCode.GAME_NOT_FOUND));
        if (!game.getGameBoard().getId().equals(gameBoard.getId())) {
            throw new GameException(GameErrorCode.GAME_NOT_FOUND);
        }
        if (game.getStatus() == GameStatus.COMPLETED) {
            throw new GameException(GameErrorCode.GAME_ALREADY_COMPLETED);
        }

        boolean wasWaiting = game.getStatus() == GameStatus.WAITING;
        Long deletedGameId = game.getId();
        List<GameDeleteResult.PlayerView> restorePlayers = command.restore()
                ? capturePlayers(game)
                : List.of();

        gameRepository.delete(game);
        if (wasWaiting) {
            resequenceWaitingQueue(gameBoard.getId());
        }
        publishMembersChanged(gameBoard.getId(), memberId);

        log.info("게임 삭제 - gameBoardId: {}, gameId: {}, wasWaiting: {}, restore: {}",
                gameBoard.getId(), deletedGameId, wasWaiting, command.restore());
        return new GameDeleteResult(deletedGameId, restorePlayers);
    }

    /**
     * 대기열 이동
     * 진행 중인 게임을 완료 기록 없이 같은 인원 그대로 대기열 맨 앞으로 되돌리고 코트를 비운다.
     * 경기 기록으로 남지 않으며 게임횟수도 증가시키지 않는다.
     *
     * @param memberId 요청자
     */
    public void moveGameToWaiting(Long memberId, GameToWaitingCommand command) {
        GameBoard gameBoard = gameBoardReader.read(command.gameBoardId());
        gameBoardAccessValidator.validateGameHost(gameBoard.getId(), memberId);

        Game game = gameRepository.findById(command.gameId())
                .orElseThrow(() -> new GameException(GameErrorCode.GAME_NOT_FOUND));
        if (!game.getGameBoard().getId().equals(gameBoard.getId())) {
            throw new GameException(GameErrorCode.GAME_NOT_FOUND);
        }
        if (game.getStatus() != GameStatus.PLAYING) {
            throw new GameException(GameErrorCode.GAME_NOT_PLAYING);
        }

        game.returnToWaiting(0);
        resequenceWaitingQueue(gameBoard.getId());
        publishMembersChanged(gameBoard.getId(), memberId);

        log.info("대기열 이동 - gameBoardId: {}, gameId: {}", gameBoard.getId(), game.getId());
    }

    private List<GameDeleteResult.PlayerView> capturePlayers(Game game) {
        return game.getPlayers().stream()
                .sorted(java.util.Comparator.comparingInt(GamePlayer::getPlayerOrder))
                .map(player -> new GameDeleteResult.PlayerView(
                        player.getGameBoardMember().getId(),
                        player.getGameBoardMember().getName(),
                        player.getGameBoardMember().getLevel(),
                        player.getPlayerOrder()))
                .toList();
    }

    /**
     * 대기열에 남은 게임들의 순서를 현재 순서 기준으로 1부터 다시 매긴다
     */
    private void resequenceWaitingQueue(Long gameBoardId) {
        List<Game> waitingGames = gameRepository
                .findByGameBoardIdAndStatusOrderByWaitingOrderAsc(gameBoardId, GameStatus.WAITING);
        int order = 1;
        for (Game waitingGame : waitingGames) {
            waitingGame.changeWaitingOrder(order++);
        }
    }

    private void publishMembersChanged(Long gameBoardId, Long memberId) {
        eventPublisher.publishEvent(GameBoardMembersChangedEvent.membersOnly(gameBoardId, memberId));
    }
}
