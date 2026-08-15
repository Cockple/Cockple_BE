package umc.cockple.demo.domain.push.service;

import umc.cockple.demo.domain.push.domain.NotificationPushOutbox;

public record ClaimedNotificationPushOutbox(
        NotificationPushOutbox outbox,
        String claimToken
) {
}
