package umc.cockple.demo.domain.chat.events;

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
        List<Long> activeSubscriberIds,
        Long messageId
) {
    public static ChatNotificationEvent create(
            Long chatRoomId,
            ChatRoomType chatRoomType,
            String notificationTitle,
            String notificationContent,
            Long senderId,
            List<Long> activeSubscriberIds
    ) {
        return ChatNotificationEvent.builder()
                .chatRoomId(chatRoomId)
                .chatRoomType(chatRoomType)
                .notificationTitle(notificationTitle)
                .notificationContent(notificationContent)
                .senderId(senderId)
                .activeSubscriberIds(activeSubscriberIds)
                .messageId(null)
                .build();
    }

    public static ChatNotificationEvent create(
            Long messageId,
            Long chatRoomId,
            ChatRoomType chatRoomType,
            String notificationTitle,
            String notificationContent,
            Long senderId,
            List<Long> activeSubscriberIds
    ) {
        return new ChatNotificationEvent(chatRoomId, chatRoomType, notificationTitle,
                notificationContent, senderId, activeSubscriberIds, messageId);
    }
}
