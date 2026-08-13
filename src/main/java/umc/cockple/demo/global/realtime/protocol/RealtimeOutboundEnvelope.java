package umc.cockple.demo.global.realtime.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RealtimeOutboundEnvelope(
        int version,
        String domain,
        String type,
        String requestId,
        Object data,
        RealtimeError error,
        Instant timestamp
) {

    public static RealtimeOutboundEnvelope success(
            String domain,
            String type,
            String requestId,
            Object data
    ) {
        return new RealtimeOutboundEnvelope(
                RealtimeProtocolVersion.CURRENT,
                domain,
                type,
                requestId,
                data,
                null,
                Instant.now()
        );
    }

    public static RealtimeOutboundEnvelope error(
            String domain,
            String requestId,
            String errorCode,
            String message
    ) {
        return new RealtimeOutboundEnvelope(
                RealtimeProtocolVersion.CURRENT,
                domain,
                "ERROR",
                requestId,
                null,
                new RealtimeError(errorCode, message),
                Instant.now()
        );
    }
}
