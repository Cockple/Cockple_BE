package umc.cockple.demo.domain.game.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
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
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GameAutoCompleteService {

    private static final int AUTO_COMPLETE_MINUTES = 30;

    private final GameRepository gameRepository;
    private final GameBoardReader gameBoardReader;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 시작 후 {@value #AUTO_COMPLETE_MINUTES}분이 지난 진행 게임을 완료 처리
     *
     * @return 자동 완료된 게임 수
     */
    public int autoCompleteStaleGames() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusMinutes(AUTO_COMPLETE_MINUTES);

        List<Game> staleGames = gameRepository.findByStatusAndStartedAtBeforeWithPlayers(
                        GameStatus.PLAYING, threshold).stream()
                .filter(game -> game.getStatus() == GameStatus.PLAYING)
                .toList();
        if (staleGames.isEmpty()) {
            return 0;
        }

        List<Long> affectedGameBoardIds = staleGames.stream()
                .map(game -> game.getGameBoard().getId())
                .distinct()
                .sorted()
                .toList();
        // 여러 보드를 변경할 때는 경기 변경 전에 같은 순서로 보드 잠금을 획득한다.
        List<GameBoard> affectedBoards = affectedGameBoardIds.stream()
                .map(gameBoardReader::readForUpdate)
                .toList();
        staleGames.forEach(this::completeInternal);
        affectedBoards.forEach(GameBoard::advanceRevision);

        // 코트가 비므로 명단뿐 아니라 보드 snapshot도 함께 전파한다. (시스템 처리이므로 actor=null)
        affectedGameBoardIds.forEach(gameBoardId -> eventPublisher.publishEvent(
                GameBoardMembersChangedEvent.membersAndBoard(gameBoardId, null)));

        log.info("게임 자동 완료 처리 - 완료 게임 수: {}, 게임판 수: {}",
                staleGames.size(), affectedGameBoardIds.size());
        return staleGames.size();
    }

    private void completeInternal(Game game) {
        game.complete(LocalDateTime.now());
        game.getPlayers().forEach(player -> player.getGameBoardMember().increaseGameCount());
    }
}
