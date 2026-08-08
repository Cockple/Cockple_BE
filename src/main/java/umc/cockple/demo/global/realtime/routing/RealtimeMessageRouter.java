package umc.cockple.demo.global.realtime.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
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

@Component
@Slf4j
public class RealtimeMessageRouter {

    private final Map<RouteKey, RealtimeDomainHandler> handlers;

    public RealtimeMessageRouter(List<RealtimeDomainHandler> domainHandlers) {
        this.handlers = registerHandlers(domainHandlers);
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

        try {
            handler.handle(requestContext, payload, responder);
        } catch (Exception e) {
            log.error(
                    "실시간 도메인 handler 처리 실패 - domain: {}, action: {}, memberId: {}, sessionId: {}",
                    routeKey.domain(), routeKey.action(), connectionContext.memberId(),
                    connectionContext.sessionId(), e
            );
            sendError(responder, RealtimeRoutingErrorCode.INTERNAL_ERROR);
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
