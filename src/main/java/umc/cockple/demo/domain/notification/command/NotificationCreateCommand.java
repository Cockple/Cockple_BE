package umc.cockple.demo.domain.notification.command;

import umc.cockple.demo.domain.notification.domain.NotificationDestination;

public record NotificationCreateCommand(
        Long recipientMemberId,
        String title,
        String content,
        String imageKey,
        String data,
        NotificationDestination destination
) {
}
