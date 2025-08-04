package umc.cockple.demo.domain.notification.dto;

import lombok.Builder;
import umc.cockple.demo.domain.notification.enums.NotificationType;

@Builder
public record AllNotificationsResponseDTO(
        Long notificationId,
        Long partyId,
        String content,
        NotificationType type,
        Boolean isRead,
        String imgUrl

) {
}
