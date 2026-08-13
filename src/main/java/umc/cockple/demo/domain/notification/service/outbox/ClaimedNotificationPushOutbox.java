package umc.cockple.demo.domain.notification.service.outbox;

import umc.cockple.demo.domain.notification.domain.outbox.NotificationPushOutbox;

public record ClaimedNotificationPushOutbox(
        NotificationPushOutbox outbox,
        String claimToken
) {
}
