package umc.cockple.demo.domain.chat.events;

import lombok.Builder;

@Builder
public record ChatMessageReadEvent(
        Long chatRoomId,
        Long lastReadMessageId,
        Long readerId
) {
    public static ChatMessageReadEvent create(Long chatRoomId, Long lastReadMessageId, Long readerId) {
        return ChatMessageReadEvent.builder()
                .chatRoomId(chatRoomId)
                .lastReadMessageId(lastReadMessageId)
                .readerId(readerId)
                .build();
    }
}
