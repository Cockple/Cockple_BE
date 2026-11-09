package umc.cockple.demo.domain.game.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.repository.GameRepository;

import java.time.LocalDateTime;

@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@RequiredArgsConstructor
@Slf4j
public class GameAutoCompleteService {

    private static final int AUTO_COMPLETE_MINUTES = 30;
    private final GameRepository gameRepository;
    private final GameAutoCompleteBoardService boardService;

    public int autoCompleteStaleGames() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(AUTO_COMPLETE_MINUTES);
        int completed = 0;
        for (Long boardId : gameRepository.findStaleGameBoardIds(GameStatus.PLAYING, threshold)) {
            try {
                completed += boardService.completeStaleGames(boardId, threshold);
            } catch (RuntimeException exception) {
                log.error("게임 자동 완료 실패 - gameBoardId: {}", boardId, exception);
            }
        }
        if (completed > 0) {
            log.info("게임 자동 완료 처리 - 완료 게임 수: {}", completed);
        }
        return completed;
    }
}
