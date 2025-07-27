package umc.cockple.demo.domain.notification.dto;

import lombok.Builder;

@Builder
public record ExistNewNotificationResponseDTO(
        Boolean existNewNotification
) {
}
