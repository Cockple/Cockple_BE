package umc.cockple.demo.domain.notification.dto;

import umc.cockple.demo.global.enums.NotificationType;

public record AllNotificationsResponseDTO(
        Long notificationId,
        String title,
        String content,
        NotificationType type,
        Boolean isRead
) {
}
