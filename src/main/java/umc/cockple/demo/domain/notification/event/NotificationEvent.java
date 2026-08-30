package umc.cockple.demo.domain.notification.event;

public record NotificationEvent(
        Long memberId,
        String title,
        String content
) {
}
