package umc.cockple.demo.domain.game.service.command;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.events.GameBoardMembersChangedEvent;
import umc.cockple.demo.domain.game.repository.GameRepository;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameAutoCompleteBoardService {

    private final GameRepository gameRepository;
    private final GameBoardReader gameBoardReader;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int completeStaleGames(Long gameBoardId, LocalDateTime threshold) {
        // 첫 일반 조회 전에 보드를 잠가, 잠금 대기 중 커밋된 변경을 조회에 반영한다.
        GameBoard board = gameBoardReader.readForUpdate(gameBoardId);
        List<Game> games = gameRepository.findStaleGamesByGameBoardIdWithPlayers(
                gameBoardId, GameStatus.PLAYING, threshold);
        if (games.isEmpty()) {
            return 0;
        }
        LocalDateTime completedAt = LocalDateTime.now();
        for (Game game : games) {
            game.complete(completedAt);
            game.getPlayers().forEach(player -> player.getGameBoardMember().increaseGameCount());
        }
        board.advanceRevision();
        eventPublisher.publishEvent(GameBoardMembersChangedEvent.membersAndBoard(gameBoardId, null));
        return games.size();
    }
}
