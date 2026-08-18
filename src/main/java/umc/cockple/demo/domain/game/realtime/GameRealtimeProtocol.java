package umc.cockple.demo.domain.game.realtime;

/**
 * 게임판 실시간(WebSocket) 프로토콜 상수
 * envelope.domain 과 응답 type 정의
 */
public final class GameRealtimeProtocol {

    public static final String DOMAIN = "GAME";

    // 응답 type
    public static final String TYPE_SUBSCRIBED = "SUBSCRIBED";
    public static final String TYPE_UNSUBSCRIBED = "UNSUBSCRIBED";
    public static final String TYPE_BOARD_UPDATED = "BOARD_UPDATED";
    public static final String TYPE_GAME_CREATED = "GAME_CREATED";
    public static final String TYPE_GAME_DELETED = "GAME_DELETED";

    private GameRealtimeProtocol() {
    }
}
