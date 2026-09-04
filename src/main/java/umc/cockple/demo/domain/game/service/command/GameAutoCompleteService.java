package umc.cockple.demo.domain.game.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.events.GameBoardMembersChangedEvent;
import umc.cockple.demo.domain.game.repository.GameRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GameAutoCompleteService {

    private static final int AUTO_COMPLETE_MINUTES = 30;

    private final GameRepository gameRepository;
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
                GameStatus.PLAYING, threshold);
        if (staleGames.isEmpty()) {
            return 0;
        }

        Set<Long> affectedGameBoardIds = new LinkedHashSet<>();
        for (Game game : staleGames) {
            if (game.getStatus() != GameStatus.PLAYING) {
                continue; // 동시성 방어: 조회 이후 상태가 바뀐 게임은 건너뛴다
            }
            completeInternal(game);
            affectedGameBoardIds.add(game.getGameBoard().getId());
        }

        // 코트가 비므로 명단뿐 아니라 보드 snapshot도 함께 전파한다. (시스템 처리이므로 actor=null)
        affectedGameBoardIds.forEach(gameBoardId ->
                eventPublisher.publishEvent(
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
