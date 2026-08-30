package umc.cockple.demo.domain.push.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        prefix = "cockple.notification-push-outbox.scheduler",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class NotificationPushOutboxScheduler {

    private final NotificationPushOutboxProcessor processor;

    @Scheduled(
            initialDelayString = "${cockple.notification-push-outbox.scheduler.initial-delay-ms:10000}",
            fixedDelayString = "${cockple.notification-push-outbox.scheduler.fixed-delay-ms:1000}"
    )
    public void processPendingPushes() {
        int processedCount = processor.processPendingBatch();
        if (processedCount > 0) {
            log.info("Notification Push outbox 배치 처리 완료 - 처리 수: {}", processedCount);
        }
    }
}
