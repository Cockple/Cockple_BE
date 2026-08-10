package umc.cockple.demo.domain.chat.presentation.realtime;

import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;

public enum ChatRealtimeAction {
    SEND(WebSocketMessageType.SEND),
    SUBSCRIBE(WebSocketMessageType.SUBSCRIBE),
    UNSUBSCRIBE(WebSocketMessageType.UNSUBSCRIBE),
    SUBSCRIBE_CHAT_LIST(WebSocketMessageType.SUBSCRIBE_CHAT_LIST),
    UNSUBSCRIBE_CHAT_LIST(WebSocketMessageType.UNSUBSCRIBE_CHAT_LIST);

    private final WebSocketMessageType commandType;

    ChatRealtimeAction(WebSocketMessageType commandType) {
        this.commandType = commandType;
    }

    public WebSocketMessageType commandType() {
        return commandType;
    }
}
