package umc.cockple.demo.domain.chat.events;

import lombok.Builder;
import umc.cockple.demo.domain.chat.enums.MessageType;

@Builder
public record ChatMessageSendEvent(
        Long chatRoomId,
        String content,
        Long senderId,
        MessageType messageType
) {
    public static ChatMessageSendEvent create(Long chatRoomId, String content, Long senderId) {
        return ChatMessageSendEvent.builder()
                .chatRoomId(chatRoomId)
                .content(content)
                .senderId(senderId)
                .messageType(MessageType.TEXT)
                .build();
    }
}
