package umc.cockple.demo.domain.notification.converter;

import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.dto.NotificationDestinationDTO;
import umc.cockple.demo.domain.notification.dto.NotificationV2ListResponseDTO;
import umc.cockple.demo.domain.notification.dto.NotificationV2ResponseDTO;

import java.util.List;

public final class NotificationV2Converter {

    private NotificationV2Converter() {
    }

    public static NotificationV2ResponseDTO toResponse(Notification notification, String imageUrl) {
        NotificationDestinationDTO destination = null;
        if (notification.getResourceType() != null
                && notification.getResourceId() != null
                && notification.getAction() != null) {
            destination = NotificationDestinationDTO.builder()
                    .resourceType(notification.getResourceType())
                    .resourceId(notification.getResourceId())
                    .action(notification.getAction())
                    .build();
        }

        return NotificationV2ResponseDTO.builder()
                .notificationId(notification.getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .isRead(notification.getIsRead())
                .imgUrl(imageUrl)
                .destination(destination)
                .build();
    }

    public static NotificationV2ListResponseDTO toListResponse(
            List<NotificationV2ResponseDTO> notifications,
            boolean hasNext,
            Long nextCursor,
            int totalElements
    ) {
        return NotificationV2ListResponseDTO.builder()
                .notifications(notifications)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .totalElements(totalElements)
                .build();
    }
}
