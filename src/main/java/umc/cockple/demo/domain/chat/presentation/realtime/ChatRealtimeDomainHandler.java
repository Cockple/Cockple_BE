package umc.cockple.demo.domain.chat.presentation.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.presentation.websocket.ChatWebSocketCommandHandler;
import umc.cockple.demo.domain.chat.service.websocket.command.ChatCommandResponder;
import umc.cockple.demo.global.realtime.routing.RealtimeDomainHandler;
import umc.cockple.demo.global.realtime.routing.RealtimeRequestContext;
import umc.cockple.demo.global.realtime.routing.RealtimeResponder;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ChatRealtimeDomainHandler implements RealtimeDomainHandler {

    public static final String DOMAIN = "CHAT";
    private static final Set<String> ACTIONS = Arrays.stream(ChatRealtimeAction.values())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());

    private final ObjectMapper objectMapper;
    private final ChatWebSocketCommandHandler commandHandler;

    @Override
    public String domain() {
        return DOMAIN;
    }

    @Override
    public Set<String> actions() {
        return ACTIONS;
    }

    @Override
    public void handle(
            RealtimeRequestContext context,
            JsonNode payload,
            RealtimeResponder responder
    ) {
        ChatRealtimeAction action = findAction(context.action());
        if (action == null || payload == null || !payload.isObject()) {
            sendInvalidPayload(responder);
            return;
        }

        ChatRealtimePayload chatPayload;
        try {
            chatPayload = objectMapper.treeToValue(
                    payload,
                    ChatRealtimePayload.class
            );
        } catch (JsonProcessingException | IllegalArgumentException e) {
            sendInvalidPayload(responder);
            return;
        }

        ChatCommandResponder commandResponder = new RealtimeChatCommandResponder(responder);
        commandHandler.handle(
                chatPayload.toCommandRequest(action),
                context.memberId(),
                commandResponder
        );
    }

    private ChatRealtimeAction findAction(String action) {
        try {
            return ChatRealtimeAction.valueOf(action);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    private void sendInvalidPayload(RealtimeResponder responder) {
        responder.sendError(
                ChatErrorCode.INVALID_REALTIME_PAYLOAD.getCode(),
                ChatErrorCode.INVALID_REALTIME_PAYLOAD.getMessage()
        );
    }
}
