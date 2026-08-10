package umc.cockple.demo.global.realtime.transport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.global.realtime.config.RealtimeWebSocketProperties;
import umc.cockple.demo.global.realtime.logging.WebSocketMdcSupport;
import umc.cockple.demo.global.realtime.protocol.RealtimeInboundEnvelope;
import umc.cockple.demo.global.realtime.routing.RealtimeConnectionContext;
import umc.cockple.demo.global.realtime.routing.RealtimeMessageRouter;
import umc.cockple.demo.global.realtime.routing.RealtimeResponder;
import umc.cockple.demo.global.realtime.routing.RealtimeRoutingErrorCode;
import umc.cockple.demo.global.realtime.session.WebSocketSessionAttributes;

@Component
@Slf4j
@RequiredArgsConstructor
public class RealtimeWebSocketRequestDispatcher {

    private final ObjectMapper objectMapper;
    private final RealtimeMessageRouter messageRouter;
    private final WebSocketRealtimeResponderFactory responderFactory;
    private final RealtimeWebSocketProperties properties;

    public void dispatch(WebSocketSession session, String payload) {
        try (WebSocketMdcSupport.MdcScope ignored = WebSocketMdcSupport.open(session)) {
            RealtimeResponder fallbackResponder = responderFactory.createInfrastructureResponder(session);

            if (payload == null || payload.length() > properties.getMaxPayloadLength()) {
                sendError(
                        fallbackResponder,
                        payload == null
                                ? RealtimeRoutingErrorCode.INVALID_MESSAGE
                                : RealtimeRoutingErrorCode.MESSAGE_TOO_LARGE
                );
                return;
            }

            RealtimeInboundEnvelope envelope;
            try {
                envelope = objectMapper.readValue(payload, RealtimeInboundEnvelope.class);
            } catch (JsonProcessingException e) {
                log.warn("실시간 WebSocket 요청 파싱 실패 - sessionId: {}", session.getId());
                sendError(fallbackResponder, RealtimeRoutingErrorCode.INVALID_MESSAGE);
                return;
            }

            if (envelope == null) {
                sendError(fallbackResponder, RealtimeRoutingErrorCode.INVALID_MESSAGE);
                return;
            }

            RealtimeResponder requestResponder = responderFactory.create(
                    session,
                    envelope.domain(),
                    envelope.requestId()
            );

            try {
                Long memberId = getMemberId(session);
                if (memberId == null) {
                    sendError(requestResponder, RealtimeRoutingErrorCode.UNAUTHORIZED);
                    return;
                }

                messageRouter.route(
                        new RealtimeConnectionContext(memberId, session.getId()),
                        envelope,
                        requestResponder
                );
            } catch (Exception e) {
                log.error("실시간 WebSocket 요청 전달 실패 - sessionId: {}", session.getId(), e);
                sendError(requestResponder, RealtimeRoutingErrorCode.INTERNAL_ERROR);
            }
        }
    }

    private Long getMemberId(WebSocketSession session) {
        Object memberId = session.getAttributes().get(WebSocketSessionAttributes.MEMBER_ID);
        return memberId instanceof Long value ? value : null;
    }

    private void sendError(RealtimeResponder responder, RealtimeRoutingErrorCode errorCode) {
        responder.sendError(errorCode.getCode(), errorCode.getMessage());
    }
}
