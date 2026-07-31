package umc.cockple.demo.domain.notification.listener.event;

public record NotificationPushRequestedEvent(
        Long memberId,
        String title,
        String content
) {
}
