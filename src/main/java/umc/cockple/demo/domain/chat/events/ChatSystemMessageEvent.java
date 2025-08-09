package umc.cockple.demo.domain.chat.events;

import java.time.LocalDateTime;

public record ChatSystemMessageEvent(
        Long chatRoomId,
        String content,
        LocalDateTime occurredAt
) {
    public static ChatSystemMessageEvent create(Long chatRoomId, String content) {
        return new ChatSystemMessageEvent(chatRoomId, content, LocalDateTime.now());
    }
}
