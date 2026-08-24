package umc.cockple.demo.domain.game.service.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.game.domain.Court;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.GamePlayer;
import umc.cockple.demo.domain.game.enums.CourtStatus;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.repository.CourtRepository;
import umc.cockple.demo.domain.game.repository.GameRepository;
import umc.cockple.demo.domain.game.service.query.result.GameBoardResult;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;
import umc.cockple.demo.domain.game.service.support.validator.GameBoardAccessValidator;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GameBoardQueryService {

    private static final List<GameStatus> ACTIVE_STATUSES = List.of(GameStatus.PLAYING, GameStatus.WAITING);

    private final GameBoardReader gameBoardReader;
    private final CourtRepository courtRepository;
    private final GameRepository gameRepository;
    private final GameBoardAccessValidator gameBoardAccessValidator;

    /**
     * 코트 보드 조회. 조회 자체는 인증된 회원이면 누구나 가능
     *
     * @param memberId 요청자
     */
    public GameBoardResult getBoard(Long memberId, Long gameBoardId) {
        GameBoard gameBoard = gameBoardReader.read(gameBoardId);
        boolean isGameHost = gameBoardAccessValidator.isGameHost(gameBoard.getId(), memberId);

        List<Court> courts = courtRepository.findByGameBoardIdOrderByCourtNoAsc(gameBoard.getId());
        List<Game> activeGames = gameRepository.findByGameBoardIdAndStatusInWithPlayers(
                gameBoard.getId(), ACTIVE_STATUSES);

        Map<Long, Game> playingGameByCourtId = activeGames.stream()
                .filter(game -> game.getStatus() == GameStatus.PLAYING && game.getCourt() != null)
                .collect(Collectors.toMap(game -> game.getCourt().getId(), Function.identity(), (a, b) -> a));

        List<GameBoardResult.CourtView> courtViews = courts.stream()
                .map(court -> toCourtView(court, playingGameByCourtId.get(court.getId())))
                .toList();

        List<GameBoardResult.WaitingView> waitingViews = activeGames.stream()
                .filter(game -> game.getStatus() == GameStatus.WAITING)
                .sorted(Comparator.comparing(Game::getWaitingOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toWaitingView)
                .toList();

        return new GameBoardResult(isGameHost, courts.size(), courtViews, waitingViews);
    }

    private GameBoardResult.CourtView toCourtView(Court court, Game playingGame) {
        boolean playing = playingGame != null;
        return new GameBoardResult.CourtView(
                court.getId(),
                court.getCourtNo(),
                court.getCourtName(),
                playing ? CourtStatus.PLAYING : CourtStatus.EMPTY,
                playing ? toGameView(playingGame) : null);
    }

    private GameBoardResult.GameView toGameView(Game game) {
        return new GameBoardResult.GameView(game.getId(), game.getStartedAt(), toPlayerViews(game));
    }

    private GameBoardResult.WaitingView toWaitingView(Game game) {
        return new GameBoardResult.WaitingView(game.getId(), game.getWaitingOrder(), toPlayerViews(game));
    }

    private List<GameBoardResult.PlayerView> toPlayerViews(Game game) {
        return game.getPlayers().stream()
                .sorted(Comparator.comparingInt(GamePlayer::getPlayerOrder))
                .map(player -> new GameBoardResult.PlayerView(
                        player.getGameBoardMember().getId(),
                        player.getGameBoardMember().getName(),
                        player.getGameBoardMember().getLevel(),
                        player.getPlayerOrder()))
                .toList();
    }
}
