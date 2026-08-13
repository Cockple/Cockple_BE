package umc.cockple.demo.domain.notification.enums.outbox;

import java.util.List;

public enum NotificationPushOutboxStatus {
    PENDING,
    PROCESSING,
    FAILED,
    DONE;

    public static List<NotificationPushOutboxStatus> retryableStatuses() {
        return List.of(PENDING, FAILED);
    }
}
