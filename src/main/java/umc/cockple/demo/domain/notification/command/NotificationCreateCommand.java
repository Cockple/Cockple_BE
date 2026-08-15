package umc.cockple.demo.domain.notification.command;

import umc.cockple.demo.domain.notification.domain.NotificationDestination;

public record NotificationCreateCommand(
        Long recipientMemberId,
        String title,
        String content,
        String imageKey,
        String data,
        NotificationDestination destination,
        String notificationKey
) {
    public NotificationCreateCommand(
            Long recipientMemberId,
            String title,
            String content,
            String imageKey,
            String data,
            NotificationDestination destination
    ) {
        this(recipientMemberId, title, content, imageKey, data, destination, null);
    }
}
