package umc.cockple.demo.domain.chat.repository.projection;

public record ChatMessageUnreadCountDTO(
        Long chatMessageId,
        Long unreadCount
) {
}
