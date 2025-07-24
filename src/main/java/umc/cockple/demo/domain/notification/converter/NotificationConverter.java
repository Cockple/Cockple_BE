package umc.cockple.demo.domain.notification.converter;

import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.dto.AllNotificationsResponseDTO;

public class NotificationConverter {

    public static AllNotificationsResponseDTO toAllNotificationResponseDTO(Notification notification) {
        return AllNotificationsResponseDTO.builder()
                .notificationId(notification.getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .type(notification.getType())
                .isRead(notification.getIsRead())
                .build();
    }
}
