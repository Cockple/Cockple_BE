package umc.cockple.demo.domain.notification.dto;

import lombok.Builder;
import umc.cockple.demo.domain.notification.enums.NotificationAction;
import umc.cockple.demo.domain.notification.enums.NotificationResourceType;

@Builder
public record NotificationDestinationDTO(
        NotificationResourceType resourceType,
        Long resourceId,
        NotificationAction action
) {
}
