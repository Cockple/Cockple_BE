package umc.cockple.demo.domain.chat.events;

import lombok.Builder;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO.FileInfo;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO.ImageInfo;
import umc.cockple.demo.domain.chat.enums.MessageType;

import java.util.List;

@Builder
public record ChatMessageSendEvent(
        Long chatRoomId,
        String content,
        List<FileInfo> files,
        List<ImageInfo> images,
        Long senderId,
        MessageType messageType
) {
    public static ChatMessageSendEvent create(
            Long chatRoomId, String content, List<FileInfo> files, List<ImageInfo> images, Long senderId) {
        return ChatMessageSendEvent.builder()
                .chatRoomId(chatRoomId)
                .content(content)
                .files(files)
                .images(images)
                .senderId(senderId)
                .messageType(MessageType.TEXT)
                .build();
    }
}
