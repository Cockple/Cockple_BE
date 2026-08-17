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
import umc.cockple.demo.domain.game.service.command.GameCommandService;
import umc.cockple.demo.domain.game.service.command.GameCourtCommandService;
import umc.cockple.demo.domain.game.service.command.model.GameCompleteCommand;
import umc.cockple.demo.domain.game.service.command.model.GameCourtMoveCommand;
import umc.cockple.demo.domain.game.service.command.model.GameCreateCommand;
import umc.cockple.demo.domain.game.service.command.model.GameStartCommand;
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
    private final GameCommandService gameCommandService;
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
                case START_GAME -> handleStartGame(context, gamePayload, responder);
                case CREATE_GAME -> handleCreateGame(context, gamePayload, responder);
                case COMPLETE_GAME -> handleCompleteGame(context, gamePayload, responder);
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
        gameBoardSubscriptionService.subscribe(gameBoardId, context.memberId(), context.sessionId());
        responder.send(GameRealtimeProtocol.TYPE_SUBSCRIBED, new SubscriptionAck(gameBoardId));
    }

    private void handleUnsubscribe(
            RealtimeRequestContext context, GameRealtimePayload payload, RealtimeResponder responder) {
        Long gameBoardId = requireGameBoardId(payload);
        gameBoardSubscriptionService.unsubscribe(gameBoardId, context.memberId(), context.sessionId());
        responder.send(GameRealtimeProtocol.TYPE_UNSUBSCRIBED, new SubscriptionAck(gameBoardId));
    }

    private void handleMoveCourt(
            RealtimeRequestContext context, GameRealtimePayload payload, RealtimeResponder responder) {
        Long gameBoardId = requireGameBoardId(payload);
        requireMoveCourtPayload(payload);

        GameCourtMoveCommand command = new GameCourtMoveCommand(
                gameBoardId, payload.courtId(), payload.targetCourtNo());
        gameCourtCommandService.moveCourt(context.memberId(), command);

        respondAndBroadcastBoard(context, gameBoardId, responder);
    }

    private void handleStartGame(
            RealtimeRequestContext context, GameRealtimePayload payload, RealtimeResponder responder) {
        Long gameBoardId = requireGameBoardId(payload);
        requireStartGamePayload(payload);

        GameStartCommand command = new GameStartCommand(
                gameBoardId, payload.gameId(), payload.courtId());
        gameCommandService.startGame(context.memberId(), command);

        respondAndBroadcastBoard(context, gameBoardId, responder);
    }

    private void handleCreateGame(
            RealtimeRequestContext context, GameRealtimePayload payload, RealtimeResponder responder) {
        Long gameBoardId = requireGameBoardId(payload);

        GameCreateCommand command = new GameCreateCommand(gameBoardId, payload.gameBoardMemberIds());
        Long gameId = gameCommandService.createGame(context.memberId(), command);

        // 호출자에게는 생성된 gameId + 최신 보드를, 나머지 구독자에게는 보드 갱신을 전달한다.
        GameBoardDTO.Response boardDto = loadBoardDto(context, gameBoardId);
        responder.send(GameRealtimeProtocol.TYPE_GAME_CREATED, new GameCreatedAck(gameId, boardDto));
        gameBoardBroadcaster.broadcastBoardUpdate(gameBoardId, boardDto, context.sessionId());
    }

    private void handleCompleteGame(
            RealtimeRequestContext context, GameRealtimePayload payload, RealtimeResponder responder) {
        Long gameBoardId = requireGameBoardId(payload);
        requireGameId(payload);

        GameCompleteCommand command = new GameCompleteCommand(gameBoardId, payload.gameId());
        Long completedGameId = gameCommandService.completeGame(context.memberId(), command);

        GameBoardDTO.Response boardDto = loadBoardDto(context, gameBoardId);
        responder.send(GameRealtimeProtocol.TYPE_GAME_COMPLETED, new GameCompletedAck(completedGameId, boardDto));
        gameBoardBroadcaster.broadcastBoardUpdate(gameBoardId, boardDto, context.sessionId());
    }

    private void respondAndBroadcastBoard(
            RealtimeRequestContext context, Long gameBoardId, RealtimeResponder responder) {
        GameBoardDTO.Response boardDto = loadBoardDto(context, gameBoardId);

        responder.send(GameRealtimeProtocol.TYPE_BOARD_UPDATED, boardDto);
        // 변경을 일으킨 세션은 위에서 직접 응답을 받았으므로 브로드캐스트 대상에서 제외한다.
        gameBoardBroadcaster.broadcastBoardUpdate(gameBoardId, boardDto, context.sessionId());
    }

    private GameBoardDTO.Response loadBoardDto(RealtimeRequestContext context, Long gameBoardId) {
        GameBoardResult board = gameBoardQueryService.getBoard(context.memberId(), gameBoardId);
        return gameBoardMapper.toResponse(board);
    }

    private Long requireGameBoardId(GameRealtimePayload payload) {
        if (payload.gameBoardId() == null) {
            throw new GameException(GameErrorCode.GAME_BOARD_ID_REQUIRED);
        }
        return payload.gameBoardId();
    }

    private void requireMoveCourtPayload(GameRealtimePayload payload) {
        // MOVE_COURT: 원본 코트 ID와 목적지 코트 번호가 모두 필요하다.
        if (payload.courtId() == null || payload.targetCourtNo() == null) {
            throw new GameException(GameErrorCode.INVALID_REALTIME_PAYLOAD);
        }
    }

    private void requireStartGamePayload(GameRealtimePayload payload) {
        // START_GAME: 시작할 대기 게임 ID와 목적지 코트 ID가 모두 필요하다.
        if (payload.gameId() == null || payload.courtId() == null) {
            throw new GameException(GameErrorCode.INVALID_REALTIME_PAYLOAD);
        }
    }

    private void requireGameId(GameRealtimePayload payload) {
        // COMPLETE_GAME: 완료할 게임 ID가 필요하다.
        if (payload.gameId() == null) {
            throw new GameException(GameErrorCode.INVALID_REALTIME_PAYLOAD);
        }
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

    private record GameCreatedAck(Long gameId, GameBoardDTO.Response board) {
    }

    private record GameCompletedAck(Long completedGameId, GameBoardDTO.Response board) {
    }
}
