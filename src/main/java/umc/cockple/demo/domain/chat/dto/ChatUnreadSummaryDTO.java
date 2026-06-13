package umc.cockple.demo.domain.chat.dto;

import lombok.Builder;

public class ChatUnreadSummaryDTO {

    @Builder
    public record Response(
            int totalUnreadCount,
            int partyUnreadCount,
            int directUnreadCount,
            boolean hasUnread
    ) {
    }
}
