package umc.cockple.demo.domain.chat.events;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
public record ChatRoomListUpdateEvent(
        Long chatRoomId,
        String content,
        LocalDateTime timestamp,
        String messageType,
        Map<Long, Integer> memberUnreadCounts
) {
    public static ChatRoomListUpdateEvent create(
            Long chatRoomId,
            String content,
            LocalDateTime timestamp,
            String messageType,
            Map<Long, Integer> memberUnreadCounts) {
        return ChatRoomListUpdateEvent.builder()
                .chatRoomId(chatRoomId)
                .content(content)
                .timestamp(timestamp)
                .messageType(messageType)
                .memberUnreadCounts(memberUnreadCounts)
                .build();
    }
}
