package umc.cockple.demo.domain.chat.dto;

import lombok.Builder;
import umc.cockple.demo.domain.chat.enums.MessageType;

import java.time.LocalDateTime;
import java.util.List;

public class ChatMessageDTO {

    @Builder
    public record MessageInfo(
            Long messageId,
            Long senderId,
            String senderName,
            String senderProfileImageUrl,
            String content,
            MessageType messageType,
            List<String> imageUrls,
            LocalDateTime timestamp,
            boolean isMyMessage
    ) {
    }
}
