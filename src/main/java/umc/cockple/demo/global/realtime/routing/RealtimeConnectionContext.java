package umc.cockple.demo.global.realtime.routing;

import java.util.Objects;

public record RealtimeConnectionContext(
        Long memberId,
        String sessionId
) {

    public RealtimeConnectionContext {
        Objects.requireNonNull(memberId, "memberId는 null일 수 없습니다.");
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId는 비어 있을 수 없습니다.");
        }
    }
}
