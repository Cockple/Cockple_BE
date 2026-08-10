package umc.cockple.demo.domain.chat.presentation.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.presentation.websocket.ChatWebSocketCommandHandler;
import umc.cockple.demo.domain.chat.service.websocket.command.ChatCommandResponder;
import umc.cockple.demo.global.realtime.protocol.RealtimeInboundEnvelope;
import umc.cockple.demo.global.realtime.protocol.RealtimeProtocolVersion;
import umc.cockple.demo.global.realtime.routing.RealtimeConnectionContext;
import umc.cockple.demo.global.realtime.routing.RealtimeMessageRouter;
import umc.cockple.demo.global.realtime.routing.RealtimeRequestContext;
import umc.cockple.demo.global.realtime.routing.RealtimeResponder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

@DisplayName("ChatRealtimeDomainHandler")
class ChatRealtimeDomainHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ChatWebSocketCommandHandler commandHandler;
    private RealtimeResponder responder;
    private ChatRealtimeDomainHandler handler;

    @BeforeEach
    void setUp() {
        commandHandler = mock(ChatWebSocketCommandHandler.class);
        responder = mock(RealtimeResponder.class);
        handler = new ChatRealtimeDomainHandler(objectMapper, commandHandler);
    }

    @Test
    @DisplayName("CHAT 도메인에 지원하는 채팅 action만 등록한다")
    void registersChatRoutes() {
        assertThat(handler.domain()).isEqualTo("CHAT");
        assertThat(handler.actions()).containsExactlyInAnyOrder(
                "SEND",
                "SUBSCRIBE",
                "UNSUBSCRIBE",
                "SUBSCRIBE_CHAT_LIST",
                "UNSUBSCRIBE_CHAT_LIST"
        );
    }

    @Test
    @DisplayName("router가 CHAT/SEND payload를 채팅 명령으로 변환해 전달한다")
    void routesSendCommand() {
        RealtimeMessageRouter router = new RealtimeMessageRouter(List.of(handler));
        RealtimeInboundEnvelope envelope = new RealtimeInboundEnvelope(
                RealtimeProtocolVersion.CURRENT,
                "chat",
                "send",
                "request-1",
                objectMapper.createObjectNode()
                        .put("chatRoomId", 20L)
                        .put("content", "hello")
                        .set("images", objectMapper.createArrayNode())
        );
        ArgumentCaptor<WebSocketMessageDTO.Request> requestCaptor =
                ArgumentCaptor.forClass(WebSocketMessageDTO.Request.class);
        ArgumentCaptor<ChatCommandResponder> responderCaptor =
                ArgumentCaptor.forClass(ChatCommandResponder.class);

        router.route(
                new RealtimeConnectionContext(10L, "session-1"),
                envelope,
                responder
        );

        then(commandHandler).should().handle(
                requestCaptor.capture(),
                eq(10L),
                responderCaptor.capture()
        );
        WebSocketMessageDTO.Request request = requestCaptor.getValue();
        assertThat(request.type()).isEqualTo(WebSocketMessageType.SEND);
        assertThat(request.chatRoomId()).isEqualTo(20L);
        assertThat(request.content()).isEqualTo("hello");
        assertThat(request.images()).isEmpty();

        responderCaptor.getValue().sendError("CHAT_ERROR", "채팅 오류");
        then(responder).should().sendError("CHAT_ERROR", "채팅 오류");
    }

    @Test
    @DisplayName("객체가 아닌 payload는 채팅 명령에 전달하지 않고 오류로 응답한다")
    void rejectsNonObjectPayload() {
        RealtimeRequestContext context = new RealtimeRequestContext(
                10L,
                "session-1",
                "request-1",
                "CHAT",
                "SEND"
        );

        handler.handle(context, TextNode.valueOf("invalid"), responder);

        then(responder).should().sendError(
                ChatErrorCode.INVALID_REALTIME_PAYLOAD.getCode(),
                ChatErrorCode.INVALID_REALTIME_PAYLOAD.getMessage()
        );
        then(commandHandler).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("채팅 명령 처리 예외는 router의 공용 내부 오류로 변환한다")
    void delegatesCommandFailureToRouterErrorBoundary() {
        RealtimeMessageRouter router = new RealtimeMessageRouter(List.of(handler));
        RealtimeInboundEnvelope envelope = new RealtimeInboundEnvelope(
                RealtimeProtocolVersion.CURRENT,
                "CHAT",
                "SUBSCRIBE",
                "request-1",
                objectMapper.createObjectNode().put("chatRoomId", 20L)
        );
        willThrow(new IllegalStateException("command failure"))
                .given(commandHandler)
                .handle(any(WebSocketMessageDTO.Request.class), eq(10L), any(ChatCommandResponder.class));

        router.route(
                new RealtimeConnectionContext(10L, "session-1"),
                envelope,
                responder
        );

        then(responder).should().sendError(
                "INTERNAL_ERROR",
                "실시간 요청 처리 중 오류가 발생했습니다."
        );
    }
}
