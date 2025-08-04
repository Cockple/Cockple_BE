package umc.cockple.demo.domain.chat.dto;

import lombok.Builder;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;

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

    public record Request(
            WebSocketMessageType type,
            Long chatRoomId,
            String content
    ) {
    }
}
