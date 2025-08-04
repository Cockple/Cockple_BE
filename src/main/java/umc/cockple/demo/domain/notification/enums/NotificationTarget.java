package umc.cockple.demo.domain.notification.enums;

public enum NotificationTarget {
    EXERCISE_DELETE(NotificationType.SIMPLE),
    EXERCISE_MODIFY(NotificationType.CHANGE),
    EXERCISE_ATTENDANCE(NotificationType.CHANGE),
    PARTY_DELETE(NotificationType.SIMPLE),
    PARTY_MODIFY(NotificationType.CHANGE),
    PARTY_INVITE(NotificationType.INVITE),
    PARTY_ACCEPTED(NotificationType.CHANGE);

    private final NotificationType defaultType;

    NotificationTarget(NotificationType defaultType) {
        this.defaultType = defaultType;
    }

    public NotificationType getDefaultType() {
        return defaultType;
    }
}
