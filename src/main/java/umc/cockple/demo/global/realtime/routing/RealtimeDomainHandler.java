package umc.cockple.demo.global.realtime.routing;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Set;

// 도메인 추가시 해당 인터페이스 구현
public interface RealtimeDomainHandler {

    String domain();

    Set<String> actions();

    void handle(
            RealtimeRequestContext context,
            JsonNode payload,
            RealtimeResponder responder
    );
}
