package umc.cockple.demo.global.realtime.routing;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Set;

public interface RealtimeDomainHandler {

    String domain();

    Set<String> actions();

    void handle(
            RealtimeRequestContext context,
            JsonNode payload,
            RealtimeResponder responder
    );
}
