package umc.cockple.demo.domain.chat.repository.projection;

public record ChatRoomUnreadCountDTO(
        Long chatRoomId,
        Long unreadCount
) {
}
