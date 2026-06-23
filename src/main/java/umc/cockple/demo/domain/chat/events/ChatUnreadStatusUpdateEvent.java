package umc.cockple.demo.domain.chat.events;

import java.util.List;

public record ChatUnreadStatusUpdateEvent(
        List<Long> targetMemberIds
) {
    public static ChatUnreadStatusUpdateEvent of(List<Long> targetMemberIds) {
        return new ChatUnreadStatusUpdateEvent(List.copyOf(targetMemberIds));
    }
}
