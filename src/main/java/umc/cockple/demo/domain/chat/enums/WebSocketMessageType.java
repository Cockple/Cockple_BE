package umc.cockple.demo.domain.chat.enums;

public enum WebSocketMessageType {
    CONNECT,
    SEND,
    SUBSCRIBE,
    UNSUBSCRIBE,
    ERROR,
    UNREAD_COUNT_UPDATE,
    CHAT_ROOM_LIST_UPDATE,
}
