package umc.cockple.demo.domain.push.command;

import umc.cockple.demo.domain.push.enums.NotificationPushChannel;
import umc.cockple.demo.domain.push.enums.NotificationPushTargetType;
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
        String activeSubscriberIdsJson,
        String deduplicationKey
) {
    public NotificationPushOutboxPayload(Long notificationId, NotificationPushChannel channel) {
        this(notificationId, channel, NotificationPushTargetType.NOTIFICATION,
                null, null, null, null, null, null, notificationId + ":" + channel);
    }
}
