package umc.cockple.demo.domain.notification.command;

import java.util.UUID;

import umc.cockple.demo.domain.notification.domain.NotificationDestination;
import umc.cockple.demo.domain.notification.domain.NotificationLegacyCompatibility;
import umc.cockple.demo.domain.notification.enums.NotificationOutboxEventType;
import umc.cockple.demo.domain.notification.enums.NotificationSource;

public record NotificationOutboxPayload(
        UUID eventId,
        NotificationOutboxEventType eventType,
        NotificationSource source,
        Long recipientMemberId,
        String title,
        String content,
        String imageKey,
        String data,
        NotificationDestination destination,
        NotificationLegacyCompatibility legacyCompatibility,
        String notificationKey
) {
}
