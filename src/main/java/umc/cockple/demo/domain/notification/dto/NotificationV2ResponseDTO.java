package umc.cockple.demo.domain.notification.dto;

import lombok.Builder;

@Builder
public record NotificationV2ResponseDTO(
        Long notificationId,
        String title,
        String content,
        Boolean isRead,
        String imgUrl,
        NotificationDestinationDTO destination
) {
}
