package umc.cockple.demo.domain.chat.repository.projection;

public record ChatMemberUnreadCountDTO(
        Long memberId,
        Long unreadCount
) {
}
