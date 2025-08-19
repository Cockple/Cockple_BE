package umc.cockple.demo.domain.chat.dto;

import lombok.Builder;
import umc.cockple.demo.domain.chat.enums.MessageType;

import java.time.LocalDateTime;
import java.util.List;

public class ChatCommonDTO {

    @Builder
    public record MessageInfo(
            Long messageId,
            Long senderId,
            String senderName,
            String senderProfileImageUrl,
            String content,
            MessageType messageType,
            List<ImageInfo> images,
            LocalDateTime timestamp,
            boolean isMyMessage
    ) {
    }

    @Builder
    public record ImageInfo(
            Long imageId,
            String imageUrl,
            Integer imgOrder,
            Boolean isEmoji,
            String originalFileName,
            Long fileSize,
            String fileType
    ) {
    }

    @Builder
    public record MemberInfo(
            Long memberId,
            String memberName,
            String profileImgUrl
    ) {
    }
}
