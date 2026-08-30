package umc.cockple.demo.domain.notification.service.outbox;

import umc.cockple.demo.domain.notification.domain.outbox.NotificationOutbox;

public record ClaimedNotificationOutbox(
        NotificationOutbox outbox,
        String claimToken
) {
}
