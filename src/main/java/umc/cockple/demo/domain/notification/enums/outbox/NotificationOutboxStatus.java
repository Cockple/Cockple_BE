package umc.cockple.demo.domain.notification.enums.outbox;

import java.util.List;

public enum NotificationOutboxStatus {
    PENDING,
    PROCESSING,
    FAILED,
    DONE,
    DEAD;

    public static List<NotificationOutboxStatus> retryableStatuses() {
        return List.of(PENDING, FAILED);
    }
}
