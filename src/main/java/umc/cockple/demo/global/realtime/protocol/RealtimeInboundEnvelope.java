package umc.cockple.demo.global.realtime.protocol;

import com.fasterxml.jackson.databind.JsonNode;

public record RealtimeInboundEnvelope(
        Integer version, // 프로토콜 버전
        String domain,
        String action,
        String requestId, // 요청 - 응답 연결하는 클라이언트 지정 ID
        JsonNode payload
) {
}
