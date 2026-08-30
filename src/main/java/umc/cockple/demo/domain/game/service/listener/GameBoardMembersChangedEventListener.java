package umc.cockple.demo.domain.game.service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import umc.cockple.demo.domain.game.events.GameBoardMembersChangedEvent;
import umc.cockple.demo.domain.game.presentation.dto.GameBoardDTO;
import umc.cockple.demo.domain.game.presentation.dto.GameBoardMemberDTO;
import umc.cockple.demo.domain.game.presentation.mapper.GameBoardMapper;
import umc.cockple.demo.domain.game.presentation.mapper.GameBoardMemberMapper;
import umc.cockple.demo.domain.game.realtime.GameRealtimeProtocol;
import umc.cockple.demo.domain.game.service.query.GameBoardMemberQueryService;
import umc.cockple.demo.domain.game.service.query.GameBoardQueryService;
import umc.cockple.demo.domain.game.service.query.model.GameBoardMemberSearchQuery;
import umc.cockple.demo.domain.game.service.query.result.GameBoardMemberResult;
import umc.cockple.demo.domain.game.service.query.result.GameBoardResult;
import umc.cockple.demo.domain.game.service.websocket.broadcast.GameBoardBroadcaster;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GameBoardMembersChangedEventListener {

    private static final GameBoardMemberSearchQuery NO_FILTERS =
            new GameBoardMemberSearchQuery(List.of(), null, null);

    private final GameBoardMemberQueryService gameBoardMemberQueryService;
    private final GameBoardMemberMapper gameBoardMemberMapper;
    private final GameBoardQueryService gameBoardQueryService;
    private final GameBoardMapper gameBoardMapper;
    private final GameBoardBroadcaster gameBoardBroadcaster;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMembersChanged(GameBoardMembersChangedEvent event) {
        GameBoardMemberDTO.Response membersDto;
        try {
            GameBoardMemberResult members = gameBoardMemberQueryService.getMembers(
                    event.gameBoardId(), NO_FILTERS);
            membersDto = gameBoardMemberMapper.toResponse(members);
        } catch (Exception e) {
            log.error("게임판 명단 snapshot 조회 실패 - gameBoardId: {}", event.gameBoardId(), e);
            return;
        }

        // REST 요청은 제외할 WebSocket 세션이 없으므로 요청자를 포함한 전체 구독 세션에 전파한다.
        broadcastSafely(
                event,
                GameRealtimeProtocol.TYPE_MEMBERS_UPDATED,
                () -> gameBoardBroadcaster.broadcastMembersUpdate(event.gameBoardId(), membersDto, null));

        if (!event.includeBoardSnapshot()) {
            return;
        }

        GameBoardDTO.Response boardDto;
        try {
            GameBoardResult board = gameBoardQueryService.getBoard(
                    event.actorMemberId(), event.gameBoardId());
            boardDto = gameBoardMapper.toResponse(board);
        } catch (Exception e) {
            log.error("게임판 snapshot 조회 실패 - gameBoardId: {}", event.gameBoardId(), e);
            return;
        }

        broadcastSafely(
                event,
                GameRealtimeProtocol.TYPE_BOARD_UPDATED,
                () -> gameBoardBroadcaster.broadcastBoardUpdate(event.gameBoardId(), boardDto, null));
    }

    private void broadcastSafely(
            GameBoardMembersChangedEvent event, String type, Runnable broadcast) {
        try {
            broadcast.run();
        } catch (Exception e) {
            // 일부 전파 실패가 이미 커밋된 REST 변경과 다른 snapshot 전파에 영향을 주지 않도록 흡수한다.
            log.error("게임판 명단 변경 {} 브로드캐스트 실패 - gameBoardId: {}",
                    type, event.gameBoardId(), e);
        }
    }
}
