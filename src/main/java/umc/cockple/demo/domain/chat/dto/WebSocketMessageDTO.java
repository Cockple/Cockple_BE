package umc.cockple.demo.domain.chat.dto;

import lombok.Builder;
import umc.cockple.demo.domain.chat.enums.WebSocketMessageType;

import java.time.LocalDateTime;
import java.util.List;

public class WebSocketMessageDTO {

    public record Request(
            WebSocketMessageType type,
            Long chatRoomId,
            String content,
            List<FileInfo> files,
            List<ImageInfo> images,
            Long lastReadMessageId
    ) {
        @Builder
        public record FileInfo(
                String fileKey,
                String originalFileName,
                Long fileSize,
                String fileType
        ) {
        }

        @Builder
        public record ImageInfo(
                String imgKey,
                Integer imgOrder,
                String originalFileName,
                Long fileSize,
                String fileType
        ) {
        }
    }

    @Builder
    public record ConnectionInfo(
            WebSocketMessageType type,
            Long memberId,
            String memberName,
            LocalDateTime timestamp,
            String message
    ) {
    }

    @Builder
    public record MessageResponse(
            WebSocketMessageType type,
            Long chatRoomId,
            Long messageId,
            String content,
            List<FileInfo> files,
            List<ChatCommonDTO.ImageInfo> images,
            Long senderId,
            String senderName,
            String senderProfileImageUrl,
            LocalDateTime timestamp,
            Integer unreadCount
    ) {
        @Builder
        public record FileInfo(
                Long fileId,
                String originalFileName,
                Long fileSize,
                String fileType
        ) {
        }
    }

    @Builder
    public record SubscriptionResponse(
            WebSocketMessageType type,
            Long chatRoomId,
            String message,
            LocalDateTime timestamp
    ) {
    }

    @Builder
    public record ErrorResponse(
            WebSocketMessageType type,
            String errorCode,
            String message
    ) {
    }

    @Builder
    public record UnreadCountUpdateMessage(
            WebSocketMessageType type,
            Long chatRoomId,
            Long messageId,
            int newUnreadCount,
            LocalDateTime timestamp
    ) {
    }

}
