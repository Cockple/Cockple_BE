package umc.cockple.demo.domain.notification.converter;

import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.dto.AllNotificationsResponseDTO;

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
}
