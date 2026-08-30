package umc.cockple.demo.global.realtime.session;

public record RealtimeEndpoint(String value) {

    public RealtimeEndpoint {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("실시간 endpoint 식별자는 비어 있을 수 없습니다.");
        }
    }

    public static RealtimeEndpoint of(String value) {
        return new RealtimeEndpoint(value);
    }
}
