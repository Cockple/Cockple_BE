package umc.cockple.demo.domain.notification.service.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        prefix = "cockple.notification-outbox.scheduler",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class NotificationOutboxScheduler {

    private final NotificationOutboxProcessor notificationOutboxProcessor;

    @Scheduled(
            initialDelayString = "${cockple.notification-outbox.scheduler.initial-delay-ms:10000}",
            fixedDelayString = "${cockple.notification-outbox.scheduler.fixed-delay-ms:1000}"
    )
    public void processPendingNotifications() {
        int processedCount = notificationOutboxProcessor.processPendingBatch();
        if (processedCount > 0) {
            log.info("Notification outbox 배치 처리 완료 - 처리 수: {}", processedCount);
        }
    }
}
