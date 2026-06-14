package umc.cockple.demo.domain.chat.dto;

import lombok.Builder;

public class ChatUnreadStatusDTO {

    @Builder
    public record Response(
            boolean hasUnread,
            boolean hasPartyUnread,
            boolean hasDirectUnread
    ) {
    }
}
