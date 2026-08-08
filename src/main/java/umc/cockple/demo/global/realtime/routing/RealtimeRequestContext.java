package umc.cockple.demo.global.realtime.routing;

public record RealtimeRequestContext(
        Long memberId,
        String sessionId,
        String requestId,
        String domain,
        String action
) {
}
