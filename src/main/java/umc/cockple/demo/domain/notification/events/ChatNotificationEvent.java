package umc.cockple.demo.domain.notification.events;

import lombok.Builder;
import umc.cockple.demo.domain.chat.enums.ChatRoomType;

import java.util.List;

@Builder
public record ChatNotificationEvent(
        Long chatRoomId,
        ChatRoomType chatRoomType,
        String notificationTitle,
        String notificationContent,
        Long senderId,
        List<Long> activeSubscriberIds
) {
    public static ChatNotificationEvent create(
            Long chatRoomId,
            ChatRoomType chatRoomType,
            String notificationTitle,
            String notificationContent,
            Long senderId,
            List<Long> activeSubscriberIds) {
        return ChatNotificationEvent.builder()
                .chatRoomId(chatRoomId)
                .chatRoomType(chatRoomType)
                .notificationTitle(notificationTitle)
                .notificationContent(notificationContent)
                .senderId(senderId)
                .activeSubscriberIds(activeSubscriberIds)
                .build();
    }
}
