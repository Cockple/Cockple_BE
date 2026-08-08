package umc.cockple.demo.global.realtime.protocol;

import com.fasterxml.jackson.databind.JsonNode;

public record RealtimeInboundEnvelope(
        Integer version,
        String domain,
        String action,
        String requestId,
        JsonNode payload
) {
}
