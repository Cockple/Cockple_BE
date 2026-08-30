package umc.cockple.demo.domain.notification.converter;

import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.dto.AllNotificationsResponseDTO;
import umc.cockple.demo.domain.notification.dto.NotificationListResponseDTO;

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

}
