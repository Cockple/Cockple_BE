package umc.cockple.demo.domain.chat.dto;

import lombok.Builder;

import java.time.LocalDateTime;

public class ChatDownloadTokenDTO {
    @Builder
    public record Response(
            String downloadToken,
            String downloadUrl,
            int expiresIn,
            LocalDateTime expiresAt
    ) {}
}
