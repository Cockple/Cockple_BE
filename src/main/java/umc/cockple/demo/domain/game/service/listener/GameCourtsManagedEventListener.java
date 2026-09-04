package umc.cockple.demo.domain.game.service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import umc.cockple.demo.domain.game.events.GameCourtsManagedEvent;
import umc.cockple.demo.domain.game.presentation.dto.GameBoardDTO;
import umc.cockple.demo.domain.game.presentation.mapper.GameBoardMapper;
import umc.cockple.demo.domain.game.service.query.GameBoardQueryService;
import umc.cockple.demo.domain.game.service.query.result.GameBoardResult;
import umc.cockple.demo.domain.game.service.websocket.broadcast.GameBoardBroadcaster;

/**
 * 코트 관리(REST) 커밋 후, 같은 게임판을 구독 중인 다른 사용자에게 최신 보드를 push
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GameCourtsManagedEventListener {

    private final GameBoardQueryService gameBoardQueryService;
    private final GameBoardMapper gameBoardMapper;
    private final GameBoardBroadcaster gameBoardBroadcaster;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCourtsManaged(GameCourtsManagedEvent event) {
        try {
            GameBoardResult board = gameBoardQueryService.getBoard(event.actorMemberId(), event.gameBoardId());
            GameBoardDTO.Response boardDto = gameBoardMapper.toResponse(board);
            // REST 요청에는 제외할 WebSocket 세션이 없으므로 구독 세션 전체에 브로드캐스트
            // isGameHost는 개인화 값이라 브로드캐스트 본문에서는 항상 false
            gameBoardBroadcaster.broadcastBoardUpdate(event.gameBoardId(), boardDto.forBroadcast(), null);
        } catch (Exception e) {
            log.error("코트 관리 브로드캐스트 실패 - gameBoardId: {}", event.gameBoardId(), e);
        }
    }
}
