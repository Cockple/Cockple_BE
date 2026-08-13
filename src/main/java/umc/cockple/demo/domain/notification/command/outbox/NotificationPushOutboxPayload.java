package umc.cockple.demo.domain.notification.command.outbox;

import umc.cockple.demo.domain.notification.enums.outbox.NotificationPushChannel;

public record NotificationPushOutboxPayload(
        Long notificationId,
        NotificationPushChannel channel
) {
}
