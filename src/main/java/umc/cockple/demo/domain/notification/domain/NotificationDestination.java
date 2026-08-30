package umc.cockple.demo.domain.notification.domain;

import umc.cockple.demo.domain.notification.enums.NotificationAction;
import umc.cockple.demo.domain.notification.enums.NotificationResourceType;

public record NotificationDestination(
        NotificationResourceType resourceType,
        Long resourceId,
        NotificationAction action
) {
}
