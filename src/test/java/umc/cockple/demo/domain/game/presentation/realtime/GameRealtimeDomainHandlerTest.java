package umc.cockple.demo.domain.game.presentation.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.presentation.dto.GameBoardDTO;
import umc.cockple.demo.domain.game.presentation.mapper.GameBoardMapper;
import umc.cockple.demo.domain.game.realtime.GameRealtimeProtocol;
import umc.cockple.demo.domain.game.service.command.GameCommandService;
import umc.cockple.demo.domain.game.service.command.GameCourtCommandService;
import umc.cockple.demo.domain.game.service.command.model.GameCreateCommand;
import umc.cockple.demo.domain.game.service.command.model.GameToWaitingCommand;
import umc.cockple.demo.domain.game.service.command.result.GameCreateResult;
import umc.cockple.demo.domain.game.service.query.GameBoardQueryService;
import umc.cockple.demo.domain.game.service.query.result.GameBoardResult;
import umc.cockple.demo.domain.game.service.websocket.broadcast.GameBoardBroadcaster;
import umc.cockple.demo.domain.game.service.websocket.subscription.GameBoardSubscriptionService;
import umc.cockple.demo.global.realtime.routing.RealtimeRequestContext;
import umc.cockple.demo.global.realtime.routing.RealtimeResponder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("GameRealtimeDomainHandler")
class GameRealtimeDomainHandlerTest {

    private static final Long MEMBER_ID = 100L;
    private static final Long GAME_BOARD_ID = 1L;
    private static final String SESSION_ID = "session-1";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private GameCommandService gameCommandService;
    private GameBoardQueryService gameBoardQueryService;
    private GameBoardMapper gameBoardMapper;
    private GameBoardBroadcaster gameBoardBroadcaster;
    private RealtimeResponder responder;
    private GameRealtimeDomainHandler handler;

    @BeforeEach
    void setUp() {
        gameCommandService = mock(GameCommandService.class);
        gameBoardQueryService = mock(GameBoardQueryService.class);
        gameBoardMapper = mock(GameBoardMapper.class);
        gameBoardBroadcaster = mock(GameBoardBroadcaster.class);
        responder = mock(RealtimeResponder.class);
        handler = new GameRealtimeDomainHandler(
                objectMapper,
                mock(GameBoardSubscriptionService.class),
                mock(GameCourtCommandService.class),
                gameCommandService,
                gameBoardQueryService,
                gameBoardMapper,
                gameBoardBroadcaster
        );
    }

    @Nested
    @DisplayName("CREATE_GAME")
    class CreateGame {

        @Test
        @DisplayName("생성 트랜잭션이 만든 보드로 요청자에게만 응답하고, 보드 재조회나 동기 브로드캐스트를 하지 않는다")
        void respondsWithBoardFromCreateResultWithoutRequeryOrBroadcast() {
            // given
            GameBoardResult board = new GameBoardResult(true, 2, List.of(), List.of());
            GameBoardDTO.Response boardDto = new GameBoardDTO.Response(true, 2, List.of(), List.of());
            given(gameCommandService.createGame(eq(MEMBER_ID), any(GameCreateCommand.class)))
                    .willReturn(new GameCreateResult(50L, board));
            given(gameBoardMapper.toResponse(board)).willReturn(boardDto);

            // when
            handler.handle(context("CREATE_GAME"), createPayload(List.of(8L, 7L)), responder);

            // then
            ArgumentCaptor<GameCreateCommand> commandCaptor = ArgumentCaptor.forClass(GameCreateCommand.class);
            then(gameCommandService).should().createGame(eq(MEMBER_ID), commandCaptor.capture());
            assertThat(commandCaptor.getValue().gameBoardId()).isEqualTo(GAME_BOARD_ID);
            assertThat(commandCaptor.getValue().gameBoardMemberIds()).containsExactly(8L, 7L);

            ArgumentCaptor<Object> ackCaptor = ArgumentCaptor.forClass(Object.class);
            then(responder).should().send(eq(GameRealtimeProtocol.TYPE_GAME_CREATED), ackCaptor.capture());
            JsonNode ack = objectMapper.valueToTree(ackCaptor.getValue());
            assertThat(ack.get("gameId").asLong()).isEqualTo(50L);
            assertThat(ack.get("board").get("courtCount").asInt()).isEqualTo(2);

            // 구독자 전파는 커밋 후 비동기 리스너 몫이다.
            verifyNoInteractions(gameBoardQueryService, gameBoardBroadcaster);
        }

        @Test
        @DisplayName("게임 생성이 도메인 예외로 실패하면 오류로 응답하고 성공 응답을 보내지 않는다")
        void respondsErrorWhenCreateFails() {
            // given
            given(gameCommandService.createGame(eq(MEMBER_ID), any(GameCreateCommand.class)))
                    .willThrow(new GameException(GameErrorCode.UNAVAILABLE_GAME_PLAYER));

            // when
            handler.handle(context("CREATE_GAME"), createPayload(List.of(7L)), responder);

            // then
            then(responder).should().sendError(
                    GameErrorCode.UNAVAILABLE_GAME_PLAYER.getCode(),
                    GameErrorCode.UNAVAILABLE_GAME_PLAYER.getMessage());
            then(responder).should(never()).send(anyString(), any());
            verifyNoInteractions(gameBoardQueryService, gameBoardBroadcaster);
        }

        @Test
        @DisplayName("gameBoardId가 없으면 게임을 만들지 않고 GAME_BOARD_ID_REQUIRED 오류로 응답한다")
        void rejectsMissingGameBoardId() {
            // given
            ObjectNode payload = objectMapper.createObjectNode();
            payload.putArray("gameBoardMemberIds").add(7L);

            // when
            handler.handle(context("CREATE_GAME"), payload, responder);

            // then
            then(responder).should().sendError(
                    GameErrorCode.GAME_BOARD_ID_REQUIRED.getCode(),
                    GameErrorCode.GAME_BOARD_ID_REQUIRED.getMessage());
            verifyNoInteractions(gameCommandService);
        }
    }

    @Nested
    @DisplayName("보드 재조회 후 응답·브로드캐스트하는 액션 (현재 동작 고정)")
    class RespondAndBroadcastBoard {

        @Test
        @DisplayName("MOVE_TO_WAITING은 요청자에게 BOARD_UPDATED로 응답하고, 요청 세션을 제외해 isGameHost=false 보드를 브로드캐스트한다")
        void moveToWaitingRespondsAndBroadcastsExcludingRequester() {
            // given
            GameBoardResult board = new GameBoardResult(true, 1, List.of(), List.of());
            GameBoardDTO.Response boardDto = new GameBoardDTO.Response(true, 1, List.of(), List.of());
            given(gameBoardQueryService.getBoard(MEMBER_ID, GAME_BOARD_ID)).willReturn(board);
            given(gameBoardMapper.toResponse(board)).willReturn(boardDto);
            ObjectNode payload = objectMapper.createObjectNode()
                    .put("gameBoardId", GAME_BOARD_ID)
                    .put("gameId", 50L);

            // when
            handler.handle(context("MOVE_TO_WAITING"), payload, responder);

            // then
            then(gameCommandService).should()
                    .moveGameToWaiting(MEMBER_ID, new GameToWaitingCommand(GAME_BOARD_ID, 50L));
            then(responder).should().send(GameRealtimeProtocol.TYPE_BOARD_UPDATED, boardDto);
            then(gameBoardBroadcaster).should()
                    .broadcastBoardUpdate(GAME_BOARD_ID, boardDto.forBroadcast(), SESSION_ID);
        }
    }

    private RealtimeRequestContext context(String action) {
        return new RealtimeRequestContext(MEMBER_ID, SESSION_ID, "request-1", "GAME", action);
    }

    private ObjectNode createPayload(List<Long> gameBoardMemberIds) {
        ObjectNode payload = objectMapper.createObjectNode().put("gameBoardId", GAME_BOARD_ID);
        gameBoardMemberIds.forEach(payload.putArray("gameBoardMemberIds")::add);
        return payload;
    }
}
