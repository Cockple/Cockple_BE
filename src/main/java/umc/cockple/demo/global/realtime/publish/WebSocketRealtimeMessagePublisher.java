package umc.cockple.demo.global.realtime.publish;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import umc.cockple.demo.global.realtime.logging.WebSocketMdcSupport;
import umc.cockple.demo.global.realtime.message.EncodedRealtimeMessage;
import umc.cockple.demo.global.realtime.message.RealtimeMessageEncoder;
import umc.cockple.demo.global.realtime.protocol.RealtimeOutboundEnvelope;
import umc.cockple.demo.global.realtime.session.RealtimeSession;
import umc.cockple.demo.global.realtime.session.RealtimeSessionRegistry;
import umc.cockple.demo.global.realtime.session.WebSocketSessionMessageSender;
import umc.cockple.demo.global.realtime.transport.RealtimeWebSocketEndpoint;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketRealtimeMessagePublisher implements RealtimeMessagePublisher {

    private final RealtimeSessionRegistry sessionRegistry;
    private final RealtimeMessageEncoder messageEncoder;
    private final WebSocketSessionMessageSender messageSender;

    @Override
    public RealtimePublishResult publish(
            Long memberId,
            String domain,
            String type,
            Object data
    ) {
        Objects.requireNonNull(memberId, "memberId는 null일 수 없습니다.");
        String normalizedDomain = normalize(domain, "domain");
        String normalizedType = normalize(type, "type");

        try (WebSocketMdcSupport.MdcScope ignored = WebSocketMdcSupport.open(memberId)) {
            List<RealtimeSession> targetSessions = sessionRegistry.findOpenSessions(
                    memberId,
                    RealtimeWebSocketEndpoint.SESSION_ENDPOINT
            );
            if (targetSessions.isEmpty()) {
                log.debug("공용 실시간 메시지 발행 대상 세션 없음 - domain: {}, type: {}", normalizedDomain, normalizedType);
                return RealtimePublishResult.noTarget();
            }

            RealtimeOutboundEnvelope envelope = RealtimeOutboundEnvelope.success(
                    normalizedDomain,
                    normalizedType,
                    null,
                    data
            );
            EncodedRealtimeMessage encodedMessage = messageEncoder.encode(envelope).orElse(null);
            if (encodedMessage == null) {
                log.error("공용 실시간 메시지 직렬화 실패 - domain: {}, type: {}", normalizedDomain, normalizedType);
                return RealtimePublishResult.failed(targetSessions.size());
            }

            int successCount = 0;
            for (RealtimeSession targetSession : targetSessions) {
                if (messageSender.send(targetSession.webSocketSession(), encodedMessage)) {
                    successCount++;
                    continue;
                }

                if (!targetSession.isOpen()) {
                    sessionRegistry.remove(memberId, targetSession.webSocketSession());
                }
            }

            RealtimePublishResult result = new RealtimePublishResult(
                    targetSessions.size(),
                    successCount
            );
            log.debug(
                    "공용 실시간 메시지 발행 완료 - domain: {}, type: {}, 대상: {}, 성공: {}, 실패: {}",
                    normalizedDomain,
                    normalizedType,
                    result.targetSessionCount(),
                    result.successCount(),
                    result.failureCount()
            );
            return result;
        }
    }

    private String normalize(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은 비어 있을 수 없습니다.");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
