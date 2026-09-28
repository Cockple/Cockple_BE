package umc.cockple.demo.domain.game.service.support.assembler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.file.service.ImageUrlResolver;
import umc.cockple.demo.domain.game.domain.Court;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.domain.GamePlayer;
import umc.cockple.demo.domain.game.enums.CourtStatus;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.service.query.result.GameBoardResult;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.ProfileImg;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 이미 조회된 코트/활성 게임으로 코트 보드 결과를 조립, DB조회 X
 */
@Component
@RequiredArgsConstructor
public class GameBoardResultAssembler {

    private final ImageUrlResolver imageUrlResolver;

    /**
     * @param courts      게임판의 코트 목록(courtNo 오름차순)
     * @param activeGames PLAYING/WAITING 게임, 순서 무관
     */
    public GameBoardResult assemble(boolean isGameHost, List<Court> courts, List<Game> activeGames) {
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
                .map(player -> {
                    GameBoardMember gameBoardMember = player.getGameBoardMember();
                    return new GameBoardResult.PlayerView(
                            gameBoardMember.getId(),
                            gameBoardMember.getName(),
                            resolveProfileImageUrl(gameBoardMember),
                            gameBoardMember.getLevel(),
                            player.getPlayerOrder());
                })
                .toList();
    }

    private String resolveProfileImageUrl(GameBoardMember gameBoardMember) {
        Member member = gameBoardMember.getMember();
        return member == null
                ? null
                : imageUrlResolver.resolve(member.getProfileImg(), ProfileImg::getImgKey);
    }
}
