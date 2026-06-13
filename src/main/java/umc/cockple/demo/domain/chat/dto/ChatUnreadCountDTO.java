package umc.cockple.demo.domain.chat.dto;

import lombok.Builder;

public class ChatUnreadCountDTO {

    @Builder
    public record Response(
            int unreadCount,
            boolean hasUnread
    ) {
    }
}
