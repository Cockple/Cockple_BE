package umc.cockple.demo.global.realtime.transport;

import umc.cockple.demo.global.realtime.session.RealtimeEndpoint;

// 경로 모음
public final class RealtimeWebSocketEndpoint {

    public static final String PATH = "/ws/realtime";
    public static final String PROTOCOL_DOMAIN = "REALTIME";
    public static final RealtimeEndpoint SESSION_ENDPOINT = RealtimeEndpoint.of("realtime");

    private RealtimeWebSocketEndpoint() {
    }
}
