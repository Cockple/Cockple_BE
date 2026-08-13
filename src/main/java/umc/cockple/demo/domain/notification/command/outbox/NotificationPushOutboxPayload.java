package umc.cockple.demo.domain.notification.command.outbox;

import umc.cockple.demo.domain.notification.enums.outbox.NotificationPushChannel;
import umc.cockple.demo.domain.notification.enums.outbox.NotificationPushTargetType;
import umc.cockple.demo.domain.chat.enums.ChatRoomType;

public record NotificationPushOutboxPayload(
        Long notificationId,
        NotificationPushChannel channel,
        NotificationPushTargetType targetType,
        Long chatRoomId,
        ChatRoomType chatRoomType,
        String title,
        String content,
        Long senderId,
        String activeSubscriberIdsJson
) {
    public NotificationPushOutboxPayload(Long notificationId, NotificationPushChannel channel) {
        this(notificationId, channel, NotificationPushTargetType.NOTIFICATION,
                null, null, null, null, null, null, null);
    }
}
