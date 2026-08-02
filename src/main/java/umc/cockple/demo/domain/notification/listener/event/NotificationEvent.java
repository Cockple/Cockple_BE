package umc.cockple.demo.domain.notification.listener.event;

public record NotificationEvent(
        Long memberId,
        String title,
        String content
) {
}
