package umc.cockple.demo.domain.notification.dto;

import umc.cockple.demo.domain.notification.domain.NotificationDestination;
import umc.cockple.demo.domain.notification.enums.NotificationType;

public record NotificationCreateCommand(
        Long recipientMemberId,
        Long partyId,
        String title,
        String content,
        NotificationType type,
        String imageKey,
        String data,
        NotificationDestination destination
) {
}
