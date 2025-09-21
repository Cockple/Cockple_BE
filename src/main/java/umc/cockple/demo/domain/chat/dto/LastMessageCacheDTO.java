package umc.cockple.demo.domain.chat.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record LastMessageCacheDTO(
        String content,
        LocalDateTime timestamp,
        String messageType
) {
}
