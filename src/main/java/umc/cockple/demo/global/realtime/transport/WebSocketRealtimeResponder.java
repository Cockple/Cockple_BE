package umc.cockple.demo.global.realtime.transport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketSession;
import umc.cockple.demo.global.realtime.message.EncodedRealtimeMessage;
import umc.cockple.demo.global.realtime.message.RealtimeMessageEncoder;
import umc.cockple.demo.global.realtime.protocol.RealtimeOutboundEnvelope;
import umc.cockple.demo.global.realtime.routing.RealtimeResponder;
import umc.cockple.demo.global.realtime.session.WebSocketSessionMessageSender;

@Slf4j
@RequiredArgsConstructor
final class WebSocketRealtimeResponder implements RealtimeResponder {

    private final WebSocketSession session;
    private final String domain;
    private final String requestId;
    private final RealtimeMessageEncoder messageEncoder;
    private final WebSocketSessionMessageSender messageSender;

    @Override
    public void send(String type, Object data) {
        sendEnvelope(RealtimeOutboundEnvelope.success(domain, type, requestId, data));
    }

    @Override
    public void sendError(String errorCode, String message) {
        sendEnvelope(RealtimeOutboundEnvelope.error(domain, requestId, errorCode, message));
    }

    private void sendEnvelope(RealtimeOutboundEnvelope envelope) {
        if (!session.isOpen()) {
            log.debug("종료된 실시간 WebSocket 세션에는 응답을 전송하지 않습니다. - sessionId: {}", session.getId());
            return;
        }

        EncodedRealtimeMessage encodedMessage = messageEncoder.encode(envelope).orElse(null);
        if (encodedMessage == null) {
            log.error("실시간 WebSocket 응답 직렬화 실패 - sessionId: {}, domain: {}", session.getId(), domain);
            return;
        }

        if (!messageSender.send(session, encodedMessage)) {
            log.warn("실시간 WebSocket 응답 전송 실패 - sessionId: {}, domain: {}", session.getId(), domain);
        }
    }
}
