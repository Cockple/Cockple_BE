package umc.cockple.demo.domain.chat.dto;

import lombok.Builder;
import umc.cockple.demo.domain.chat.enums.MessageType;

import java.time.LocalDateTime;
import java.util.List;

public class ChatMessageDTO {

    @Builder
    public record Response(
            List<MessageInfo> messages,
            Boolean hasNext,
            Long nextCursor,
            int totalElements
    ) {
    }

    @Builder
    public record MessageInfo(
            Long messageId,
            Long senderId,
            String senderName,
            String senderProfileImageUrl,
            String content,
            MessageType messageType,
            List<ChatCommonDTO.ImageInfo> images,
            LocalDateTime timestamp,
            boolean isMyMessage
    ) {
        public static MessageInfo from(ChatCommonDTO.MessageInfo common) {
            return MessageInfo.builder()
                    .messageId(common.messageId())
                    .senderId(common.senderId())
                    .senderName(common.senderName())
                    .senderProfileImageUrl(common.senderProfileImageUrl())
                    .content(common.content())
                    .messageType(common.messageType())
                    .images(common.images())
                    .timestamp(common.timestamp())
                    .isMyMessage(common.isMyMessage())
                    .build();
        }
    }
}
