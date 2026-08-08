package umc.cockple.demo.global.realtime.routing;

public interface RealtimeResponder {

    void send(String type, Object data);

    void sendError(String errorCode, String message);
}
