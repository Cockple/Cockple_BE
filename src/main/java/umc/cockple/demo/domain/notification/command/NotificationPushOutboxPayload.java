package umc.cockple.demo.domain.notification.command;

import umc.cockple.demo.domain.notification.enums.NotificationPushChannel;

public record NotificationPushOutboxPayload(
        Long notificationId,
        NotificationPushChannel channel
) {
}
