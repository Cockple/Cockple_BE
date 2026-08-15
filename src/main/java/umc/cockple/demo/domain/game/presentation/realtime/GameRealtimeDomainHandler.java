package umc.cockple.demo.domain.game.presentation.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.presentation.dto.GameBoardDTO;
import umc.cockple.demo.domain.game.presentation.mapper.GameBoardMapper;
import umc.cockple.demo.domain.game.realtime.GameRealtimeProtocol;
import umc.cockple.demo.domain.game.service.command.GameCourtCommandService;
import umc.cockple.demo.domain.game.service.command.model.GameCourtMoveCommand;
import umc.cockple.demo.domain.game.service.query.GameBoardQueryService;
import umc.cockple.demo.domain.game.service.query.result.GameBoardResult;
import umc.cockple.demo.domain.game.service.websocket.broadcast.GameBoardBroadcaster;
import umc.cockple.demo.domain.game.service.websocket.subscription.GameBoardSubscriptionService;
import umc.cockple.demo.global.realtime.routing.RealtimeDomainHandler;
import umc.cockple.demo.global.realtime.routing.RealtimeRequestContext;
import umc.cockple.demo.global.realtime.routing.RealtimeResponder;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class GameRealtimeDomainHandler implements RealtimeDomainHandler {

    private static final Set<String> ACTIONS = Arrays.stream(GameRealtimeAction.values())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());

    private final ObjectMapper objectMapper;
    private final GameBoardSubscriptionService gameBoardSubscriptionService;
    private final GameCourtCommandService gameCourtCommandService;
    private final GameBoardQueryService gameBoardQueryService;
    private final GameBoardMapper gameBoardMapper;
    private final GameBoardBroadcaster gameBoardBroadcaster;

    @Override
    public String domain() {
        return GameRealtimeProtocol.DOMAIN;
    }

    @Override
    public Set<String> actions() {
        return ACTIONS;
    }

    @Override
    public void handle(
            RealtimeRequestContext context,
            JsonNode payload,
            RealtimeResponder responder
    ) {
        GameRealtimeAction action = findAction(context.action());
        if (action == null || payload == null || !payload.isObject()) {
            sendInvalidPayload(responder);
            return;
        }

        GameRealtimePayload gamePayload;
        try {
            gamePayload = objectMapper.treeToValue(payload, GameRealtimePayload.class);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            sendInvalidPayload(responder);
            return;
        }

        try {
            switch (action) {
                case SUBSCRIBE -> handleSubscribe(context, gamePayload, responder);
                case UNSUBSCRIBE -> handleUnsubscribe(context, gamePayload, responder);
                case MOVE_COURT -> handleMoveCourt(context, gamePayload, responder);
            }
        } catch (GameException e) {
            log.warn("게임 실시간 처리 실패 - action: {}, memberId: {}, 이유: {}",
                    action, context.memberId(), e.getErrorReason().getMessage());
            responder.sendError(e.getErrorReason().getCode(), e.getErrorReason().getMessage());
        }
        // 그 외 RuntimeException 은 공용 라우터가 INTERNAL_ERROR 로 응답
    }

    private void handleSubscribe(
            RealtimeRequestContext context, GameRealtimePayload payload, RealtimeResponder responder) {
        Long gameBoardId = requireGameBoardId(payload);
        gameBoardSubscriptionService.subscribe(gameBoardId, context.memberId());
        responder.send(GameRealtimeProtocol.TYPE_SUBSCRIBED, new SubscriptionAck(gameBoardId));
    }

    private void handleUnsubscribe(
            RealtimeRequestContext context, GameRealtimePayload payload, RealtimeResponder responder) {
        Long gameBoardId = requireGameBoardId(payload);
        gameBoardSubscriptionService.unsubscribe(gameBoardId, context.memberId());
        responder.send(GameRealtimeProtocol.TYPE_UNSUBSCRIBED, new SubscriptionAck(gameBoardId));
    }

    private void handleMoveCourt(
            RealtimeRequestContext context, GameRealtimePayload payload, RealtimeResponder responder) {
        Long gameBoardId = requireGameBoardId(payload);

        GameCourtMoveCommand command = new GameCourtMoveCommand(
                gameBoardId, payload.courtId(), payload.targetCourtNo());
        gameCourtCommandService.moveCourt(context.memberId(), command);

        // 변경 후 최신 보드 스냅샷을 만들어, 호출자에게 ack + 나머지 구독자에게 브로드캐스트
        GameBoardResult board = gameBoardQueryService.getBoard(context.memberId(), gameBoardId);
        GameBoardDTO.Response boardDto = gameBoardMapper.toResponse(board);

        responder.send(GameRealtimeProtocol.TYPE_BOARD_UPDATED, boardDto);
        gameBoardBroadcaster.broadcastBoardUpdate(gameBoardId, boardDto, context.memberId());
    }

    private Long requireGameBoardId(GameRealtimePayload payload) {
        if (payload.gameBoardId() == null) {
            throw new GameException(GameErrorCode.GAME_BOARD_ID_REQUIRED);
        }
        return payload.gameBoardId();
    }

    private GameRealtimeAction findAction(String action) {
        try {
            return GameRealtimeAction.valueOf(action);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    private void sendInvalidPayload(RealtimeResponder responder) {
        responder.sendError(
                GameErrorCode.INVALID_REALTIME_PAYLOAD.getCode(),
                GameErrorCode.INVALID_REALTIME_PAYLOAD.getMessage());
    }

    private record SubscriptionAck(Long gameBoardId) {
    }
}
