package umc.cockple.demo.domain.push.enums;

import java.util.List;

public enum NotificationPushOutboxStatus {
    PENDING,
    PROCESSING,
    FAILED,
    DONE,
    DEAD;

    public static List<NotificationPushOutboxStatus> retryableStatuses() {
        return List.of(PENDING, FAILED);
    }
}
