package umc.cockple.demo.domain.notification.events;

public record NotificationEvent(
        Long memberId,
        String title,
        String content
) {
}
