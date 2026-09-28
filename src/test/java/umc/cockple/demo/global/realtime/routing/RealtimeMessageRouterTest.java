package umc.cockple.demo.global.realtime.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import umc.cockple.demo.global.realtime.protocol.RealtimeInboundEnvelope;
import umc.cockple.demo.global.realtime.protocol.RealtimeProtocolVersion;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@DisplayName("RealtimeMessageRouter")
class RealtimeMessageRouterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RealtimeConnectionContext connectionContext =
            new RealtimeConnectionContext(10L, "session-1");

    @Test
    @DisplayName("domain과 action에 등록된 handler에 요청 context와 payload를 전달한다")
    void routeDelegatesToRegisteredDomainHandler() {
        RealtimeDomainHandler chatHandler = handler("CHAT", "SEND", "SUBSCRIBE");
        RealtimeResponder responder = mock(RealtimeResponder.class);
        RealtimeMessageRouter router = new RealtimeMessageRouter(List.of(chatHandler), new SimpleMeterRegistry());
        JsonNode payload = objectMapper.createObjectNode()
                .put("chatRoomId", 20L)
                .put("content", "hello");
        RealtimeInboundEnvelope envelope = envelope("chat", "send", "request-1", payload);

        router.route(connectionContext, envelope, responder);

        ArgumentCaptor<RealtimeRequestContext> contextCaptor =
                ArgumentCaptor.forClass(RealtimeRequestContext.class);
        then(chatHandler).should().handle(contextCaptor.capture(), org.mockito.ArgumentMatchers.eq(payload),
                org.mockito.ArgumentMatchers.eq(responder));
        RealtimeRequestContext context = contextCaptor.getValue();
        assertThat(context.memberId()).isEqualTo(10L);
        assertThat(context.sessionId()).isEqualTo("session-1");
        assertThat(context.requestId()).isEqualTo("request-1");
        assertThat(context.domain()).isEqualTo("CHAT");
        assertThat(context.action()).isEqualTo("SEND");
    }

    @Test
    @DisplayName("서로 다른 도메인은 같은 action 이름을 독립적으로 등록할 수 있다")
    void routeSeparatesSameActionAcrossDomains() {
        RealtimeDomainHandler chatHandler = handler("CHAT", "SUBSCRIBE");
        RealtimeDomainHandler gameHandler = handler("GAME", "SUBSCRIBE");
        RealtimeResponder responder = mock(RealtimeResponder.class);
        RealtimeMessageRouter router = new RealtimeMessageRouter(List.of(chatHandler, gameHandler), new SimpleMeterRegistry());

        router.route(connectionContext, envelope("GAME", "SUBSCRIBE", "request-1", null), responder);

        then(gameHandler).should().handle(
                org.mockito.ArgumentMatchers.any(RealtimeRequestContext.class),
                org.mockito.ArgumentMatchers.eq(NullNode.getInstance()),
                org.mockito.ArgumentMatchers.eq(responder)
        );
        then(chatHandler).should(never()).handle(
                org.mockito.ArgumentMatchers.any(RealtimeRequestContext.class),
                org.mockito.ArgumentMatchers.any(JsonNode.class),
                org.mockito.ArgumentMatchers.any(RealtimeResponder.class)
        );
    }

    @Test
    @DisplayName("알 수 없는 domain과 action은 handler 호출 없이 표준 오류로 응답한다")
    void routeSendsUnknownRouteError() {
        RealtimeDomainHandler chatHandler = handler("CHAT", "SEND");
        RealtimeResponder responder = mock(RealtimeResponder.class);
        RealtimeMessageRouter router = new RealtimeMessageRouter(List.of(chatHandler), new SimpleMeterRegistry());

        router.route(connectionContext, envelope("GAME", "START", "request-1", null), responder);

        then(responder).should().sendError(
                RealtimeRoutingErrorCode.UNKNOWN_ROUTE.getCode(),
                RealtimeRoutingErrorCode.UNKNOWN_ROUTE.getMessage()
        );
        then(chatHandler).should(never()).handle(
                org.mockito.ArgumentMatchers.any(RealtimeRequestContext.class),
                org.mockito.ArgumentMatchers.any(JsonNode.class),
                org.mockito.ArgumentMatchers.any(RealtimeResponder.class)
        );
    }

    @Test
    @DisplayName("필수 필드가 없는 요청은 INVALID_MESSAGE 오류로 응답한다")
    void routeRejectsMessageWithoutRequiredFields() {
        RealtimeResponder responder = mock(RealtimeResponder.class);
        RealtimeMessageRouter router = new RealtimeMessageRouter(List.of(), new SimpleMeterRegistry());
        RealtimeInboundEnvelope envelope = new RealtimeInboundEnvelope(
                RealtimeProtocolVersion.CURRENT,
                "CHAT",
                "SEND",
                " ",
                null
        );

        router.route(connectionContext, envelope, responder);

        then(responder).should().sendError(
                RealtimeRoutingErrorCode.INVALID_MESSAGE.getCode(),
                RealtimeRoutingErrorCode.INVALID_MESSAGE.getMessage()
        );
    }

    @Test
    @DisplayName("현재 버전이 아닌 요청은 UNSUPPORTED_VERSION 오류로 응답한다")
    void routeRejectsUnsupportedProtocolVersion() {
        RealtimeResponder responder = mock(RealtimeResponder.class);
        RealtimeMessageRouter router = new RealtimeMessageRouter(List.of(), new SimpleMeterRegistry());
        RealtimeInboundEnvelope envelope = new RealtimeInboundEnvelope(
                999,
                "CHAT",
                "SEND",
                "request-1",
                null
        );

        router.route(connectionContext, envelope, responder);

        then(responder).should().sendError(
                RealtimeRoutingErrorCode.UNSUPPORTED_VERSION.getCode(),
                RealtimeRoutingErrorCode.UNSUPPORTED_VERSION.getMessage()
        );
    }

    @Test
    @DisplayName("handler 예외는 외부로 노출하지 않고 INTERNAL_ERROR로 변환한다")
    void routeConvertsHandlerExceptionToInternalError() {
        RealtimeDomainHandler chatHandler = handler("CHAT", "SEND");
        RealtimeResponder responder = mock(RealtimeResponder.class);
        RealtimeMessageRouter router = new RealtimeMessageRouter(List.of(chatHandler), new SimpleMeterRegistry());
        willThrow(new IllegalStateException("database details"))
                .given(chatHandler)
                .handle(
                        org.mockito.ArgumentMatchers.any(RealtimeRequestContext.class),
                        org.mockito.ArgumentMatchers.any(JsonNode.class),
                        org.mockito.ArgumentMatchers.eq(responder)
                );

        router.route(connectionContext, envelope("CHAT", "SEND", "request-1", null), responder);

        then(responder).should().sendError(
                RealtimeRoutingErrorCode.INTERNAL_ERROR.getCode(),
                RealtimeRoutingErrorCode.INTERNAL_ERROR.getMessage()
        );
    }

    @Test
    @DisplayName("handler 처리 시간을 domain, action, outcome 태그로 기록한다")
    void routeRecordsHandlerDuration() {
        RealtimeDomainHandler gameHandler = handler("GAME", "CREATE_GAME");
        RealtimeResponder responder = mock(RealtimeResponder.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RealtimeMessageRouter router = new RealtimeMessageRouter(List.of(gameHandler), meterRegistry);

        router.route(connectionContext, envelope("game", "create_game", "request-1", null), responder);

        Timer timer = meterRegistry.find(RealtimeMessageRouter.HANDLER_DURATION_METRIC)
                .tags("domain", "GAME", "action", "CREATE_GAME", "outcome", "success")
                .timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("handler 예외도 outcome=error로 처리 시간을 기록한다")
    void routeRecordsHandlerDurationOnFailure() {
        RealtimeDomainHandler chatHandler = handler("CHAT", "SEND");
        RealtimeResponder responder = mock(RealtimeResponder.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RealtimeMessageRouter router = new RealtimeMessageRouter(List.of(chatHandler), meterRegistry);
        willThrow(new IllegalStateException("boom"))
                .given(chatHandler)
                .handle(
                        org.mockito.ArgumentMatchers.any(RealtimeRequestContext.class),
                        org.mockito.ArgumentMatchers.any(JsonNode.class),
                        org.mockito.ArgumentMatchers.eq(responder)
                );

        router.route(connectionContext, envelope("CHAT", "SEND", "request-1", null), responder);

        Timer timer = meterRegistry.find(RealtimeMessageRouter.HANDLER_DURATION_METRIC)
                .tags("domain", "CHAT", "action", "SEND", "outcome", "error")
                .timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("라우팅되지 않은 요청은 처리 시간을 기록하지 않는다")
    void routeDoesNotRecordUnknownRoute() {
        RealtimeResponder responder = mock(RealtimeResponder.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RealtimeMessageRouter router = new RealtimeMessageRouter(List.of(), meterRegistry);

        router.route(connectionContext, envelope("GAME", "UNKNOWN", "request-1", null), responder);

        assertThat(meterRegistry.find(RealtimeMessageRouter.HANDLER_DURATION_METRIC).timers()).isEmpty();
    }

    @Test
    @DisplayName("동일한 domain과 action을 중복 등록하면 시작 시점에 실패한다")
    void constructorRejectsDuplicateRoute() {
        RealtimeDomainHandler firstHandler = handler("CHAT", "SEND");
        RealtimeDomainHandler duplicateHandler = handler("chat", "send");

        assertThatThrownBy(() -> new RealtimeMessageRouter(List.of(firstHandler, duplicateHandler), new SimpleMeterRegistry()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("중복된 실시간 route")
                .hasMessageContaining("CHAT/SEND");
    }

    private RealtimeDomainHandler handler(String domain, String... actions) {
        RealtimeDomainHandler handler = mock(RealtimeDomainHandler.class);
        given(handler.domain()).willReturn(domain);
        given(handler.actions()).willReturn(Set.of(actions));
        return handler;
    }

    private RealtimeInboundEnvelope envelope(
            String domain,
            String action,
            String requestId,
            JsonNode payload
    ) {
        return new RealtimeInboundEnvelope(
                RealtimeProtocolVersion.CURRENT,
                domain,
                action,
                requestId,
                payload
        );
    }
}
