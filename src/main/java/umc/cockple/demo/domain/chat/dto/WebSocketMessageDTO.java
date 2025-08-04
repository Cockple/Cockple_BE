package umc.cockple.demo.domain.chat.dto;

import lombok.Builder;

import java.time.LocalDateTime;

public class WebSocketMessageDTO {

    @Builder
    public record ConnectionInfo(
            String type,
            Long memberId,
            String memberName,
            LocalDateTime connectedAt,
            String message) {
    }
}
