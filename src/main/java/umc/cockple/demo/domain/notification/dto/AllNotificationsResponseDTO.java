package umc.cockple.demo.domain.notification.dto;

import lombok.Builder;
import umc.cockple.demo.global.enums.NotificationType;

@Builder
public record AllNotificationsResponseDTO(
        Long notificationId,
        String title,
        String content,
        NotificationType type,
        Boolean isRead
) {
}
