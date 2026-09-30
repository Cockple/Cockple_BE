package umc.cockple.demo.global.realtime.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import umc.cockple.demo.global.realtime.protocol.RealtimeInboundEnvelope;
import umc.cockple.demo.global.realtime.protocol.RealtimeProtocolVersion;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// 도메인이 추가되어도 RealtimeDomainHandler를 구현하고 @Component로 등록하면 자동으로 라우팅되도록 설계
@Component
@Slf4j
public class RealtimeMessageRouter {

    static final String HANDLER_DURATION_METRIC = "realtime.handler.duration";
    static final String OUTCOME_SUCCESS = "success";
    static final String OUTCOME_ERROR = "error";
    static final String EXCEPTION_NONE = "none";

    private final Map<RouteKey, RealtimeDomainHandler> handlers;
    private final MeterRegistry meterRegistry;

    public RealtimeMessageRouter(List<RealtimeDomainHandler> domainHandlers, MeterRegistry meterRegistry) {
        this.handlers = registerHandlers(domainHandlers);
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry는 null일 수 없습니다.");
    }

    public void route(
            RealtimeConnectionContext connectionContext,
            RealtimeInboundEnvelope envelope,
            RealtimeResponder responder
    ) {
        Objects.requireNonNull(connectionContext, "connectionContext는 null일 수 없습니다.");
        Objects.requireNonNull(responder, "responder는 null일 수 없습니다.");

        if (!hasRequiredFields(envelope)) {
            sendError(responder, RealtimeRoutingErrorCode.INVALID_MESSAGE);
            return;
        }

        if (envelope.version() != RealtimeProtocolVersion.CURRENT) {
            sendError(responder, RealtimeRoutingErrorCode.UNSUPPORTED_VERSION);
            return;
        }

        RouteKey routeKey = RouteKey.of(envelope.domain(), envelope.action());
        RealtimeDomainHandler handler = handlers.get(routeKey);
        if (handler == null) {
            sendError(responder, RealtimeRoutingErrorCode.UNKNOWN_ROUTE);
            return;
        }

        RealtimeRequestContext requestContext = new RealtimeRequestContext(
                connectionContext.memberId(),
                connectionContext.sessionId(),
                envelope.requestId(),
                routeKey.domain(),
                routeKey.action()
        );
        JsonNode payload = envelope.payload() == null ? NullNode.getInstance() : envelope.payload();

        ErrorTrackingResponder trackingResponder = new ErrorTrackingResponder(responder);
        Timer.Sample sample = Timer.start(meterRegistry);
        String exception = EXCEPTION_NONE;
        try {
            handler.handle(requestContext, payload, trackingResponder);
        } catch (Exception e) {
            exception = e.getClass().getSimpleName();
            log.error(
                    "실시간 도메인 handler 처리 실패 - domain: {}, action: {}, memberId: {}, sessionId: {}",
                    routeKey.domain(), routeKey.action(), connectionContext.memberId(),
                    connectionContext.sessionId(), e
            );
            sendError(trackingResponder, RealtimeRoutingErrorCode.INTERNAL_ERROR);
        } finally {
            sample.stop(Timer.builder(HANDLER_DURATION_METRIC)
                    .tag("domain", routeKey.domain())
                    .tag("action", routeKey.action())
                    .tag("outcome", trackingResponder.errorSent() ? OUTCOME_ERROR : OUTCOME_SUCCESS)
                    .tag("exception", exception)
                    .register(meterRegistry));
        }
    }

    private Map<RouteKey, RealtimeDomainHandler> registerHandlers(
            List<RealtimeDomainHandler> domainHandlers
    ) {
        Objects.requireNonNull(domainHandlers, "domainHandlers는 null일 수 없습니다.");
        Map<RouteKey, RealtimeDomainHandler> registeredHandlers = new HashMap<>();

        for (RealtimeDomainHandler handler : domainHandlers) {
            Objects.requireNonNull(handler, "RealtimeDomainHandler는 null일 수 없습니다.");
            Set<String> actions = handler.actions();
            if (actions == null || actions.isEmpty()) {
                throw new IllegalStateException("실시간 handler는 하나 이상의 action을 등록해야 합니다.");
            }

            for (String action : actions) {
                RouteKey routeKey = RouteKey.of(handler.domain(), action);
                RealtimeDomainHandler previous = registeredHandlers.putIfAbsent(routeKey, handler);
                if (previous != null) {
                    throw new IllegalStateException(
                            "중복된 실시간 route입니다: " + routeKey.domain() + "/" + routeKey.action()
                    );
                }
            }
        }

        return Map.copyOf(registeredHandlers);
    }

    private boolean hasRequiredFields(RealtimeInboundEnvelope envelope) {
        return envelope != null
                && envelope.version() != null
                && isNotBlank(envelope.domain())
                && isNotBlank(envelope.action())
                && isNotBlank(envelope.requestId());
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private void sendError(RealtimeResponder responder, RealtimeRoutingErrorCode errorCode) {
        responder.sendError(errorCode.getCode(), errorCode.getMessage());
    }

    private static final class ErrorTrackingResponder implements RealtimeResponder {

        private final RealtimeResponder delegate;
        private volatile boolean errorSent;

        private ErrorTrackingResponder(RealtimeResponder delegate) {
            this.delegate = delegate;
        }

        @Override
        public void send(String type, Object data) {
            delegate.send(type, data);
        }

        @Override
        public void sendError(String errorCode, String message) {
            errorSent = true;
            delegate.sendError(errorCode, message);
        }

        private boolean errorSent() {
            return errorSent;
        }
    }

    private record RouteKey(String domain, String action) {

        private static RouteKey of(String domain, String action) {
            if (domain == null || domain.isBlank() || action == null || action.isBlank()) {
                throw new IllegalStateException("실시간 route의 domain과 action은 비어 있을 수 없습니다.");
            }
            return new RouteKey(normalize(domain), normalize(action));
        }

        private static String normalize(String value) {
            return value.trim().toUpperCase(Locale.ROOT);
        }
    }
}
