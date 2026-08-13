package umc.cockple.demo.global.realtime.transport;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.global.realtime.message.RealtimeMessageEncoder;
import umc.cockple.demo.global.realtime.routing.RealtimeResponder;
import umc.cockple.demo.global.realtime.session.WebSocketSessionMessageSender;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class WebSocketRealtimeResponderFactory {

    private final RealtimeMessageEncoder messageEncoder;
    private final WebSocketSessionMessageSender messageSender;

    public RealtimeResponder create(WebSocketSession session, String domain, String requestId) {
        return new WebSocketRealtimeResponder(
                session,
                normalizeDomain(domain),
                requestId,
                messageEncoder,
                messageSender
        );
    }

    public RealtimeResponder createInfrastructureResponder(WebSocketSession session) {
        return create(session, RealtimeWebSocketEndpoint.PROTOCOL_DOMAIN, null);
    }

    private String normalizeDomain(String domain) {
        if (domain == null || domain.isBlank()) {
            return RealtimeWebSocketEndpoint.PROTOCOL_DOMAIN;
        }
        return domain.trim().toUpperCase(Locale.ROOT);
    }
}
