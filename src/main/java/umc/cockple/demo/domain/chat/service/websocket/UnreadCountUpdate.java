package umc.cockple.demo.domain.chat.service.websocket;

public record UnreadCountUpdate(
        Long messageId,
        int newUnreadCount
) {
}
