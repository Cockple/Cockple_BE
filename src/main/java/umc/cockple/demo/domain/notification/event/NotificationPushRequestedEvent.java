package umc.cockple.demo.domain.notification.event;

public record NotificationPushRequestedEvent(
        Long memberId,
        String title,
        String content
) {
}
