package umc.cockple.demo.domain.chat.events;

import lombok.Builder;

import java.util.List;

@Builder
public record ChatMessageReadEvent(
        Long chatRoomId,
        Long messageId,
        List<Long> memberIds
) {
}
