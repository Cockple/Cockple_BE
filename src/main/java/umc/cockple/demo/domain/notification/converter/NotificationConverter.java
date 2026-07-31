package umc.cockple.demo.domain.notification.converter;

import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.dto.AllNotificationsResponseDTO;
import umc.cockple.demo.domain.notification.dto.NotificationListResponseDTO;
import umc.cockple.demo.domain.notification.dto.NotificationDestinationDTO;
import umc.cockple.demo.domain.notification.dto.NotificationV2ListResponseDTO;
import umc.cockple.demo.domain.notification.dto.NotificationV2ResponseDTO;

import java.util.List;

public class NotificationConverter {

    public static AllNotificationsResponseDTO toAllNotificationResponseDTO(Notification notification, String imgUrl) {
        return AllNotificationsResponseDTO.builder()
                .notificationId(notification.getId())
                .partyId(notification.getPartyId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .type(notification.getType())
                .isRead(notification.getIsRead())
                .imgUrl(imgUrl)
                .data(notification.getData())
                .build();
    }

    public static NotificationListResponseDTO toNotificationListResponse(
            List<AllNotificationsResponseDTO> notifications, boolean hasNext, Long nextCursor, int totalElements) {
        return NotificationListResponseDTO.builder()
                .notifications(notifications)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .totalElements(totalElements)
                .build();
    }

    public static NotificationV2ResponseDTO toNotificationV2ResponseDTO(Notification notification, String imgUrl) {
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
                .imgUrl(imgUrl)
                .destination(destination)
                .build();
    }

    public static NotificationV2ListResponseDTO toNotificationV2ListResponse(
            List<NotificationV2ResponseDTO> notifications,
            boolean hasNext,
            Long nextCursor,
            int totalElements) {
        return NotificationV2ListResponseDTO.builder()
                .notifications(notifications)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .totalElements(totalElements)
                .build();
    }
}
