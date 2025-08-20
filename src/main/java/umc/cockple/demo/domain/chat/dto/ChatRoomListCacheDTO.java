package umc.cockple.demo.domain.chat.dto;

import lombok.Builder;

import java.time.LocalDateTime;

public class ChatRoomListCacheDTO {

    @Builder
    public record LastMessageCache(
            Long messageId,
            String content,
            LocalDateTime timestamp,
            String messageType
    ) {
    }
}
