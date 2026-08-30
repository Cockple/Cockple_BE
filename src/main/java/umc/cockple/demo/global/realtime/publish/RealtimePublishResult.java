package umc.cockple.demo.global.realtime.publish;

public record RealtimePublishResult(
        int targetSessionCount,
        int successCount
) {

    public RealtimePublishResult {
        if (targetSessionCount < 0 || successCount < 0 || successCount > targetSessionCount) {
            throw new IllegalArgumentException("실시간 발행 결과의 세션 수가 올바르지 않습니다.");
        }
    }

    public static RealtimePublishResult noTarget() {
        return new RealtimePublishResult(0, 0);
    }

    public static RealtimePublishResult failed(int targetSessionCount) {
        return new RealtimePublishResult(targetSessionCount, 0);
    }

    public int failureCount() {
        return targetSessionCount - successCount;
    }

    public boolean deliveredToAnySession() {
        return successCount > 0;
    }

    public boolean deliveredToAllSessions() {
        return targetSessionCount > 0 && successCount == targetSessionCount;
    }
}
