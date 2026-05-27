package umc.cockple.demo.domain.file.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        prefix = "cockple.object-storage-delete-outbox.scheduler",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ObjectStorageDeleteOutboxScheduler {

    private final ObjectStorageDeleteOutboxProcessor objectStorageDeleteOutboxProcessor;

    @Scheduled(
            initialDelayString = "${cockple.object-storage-delete-outbox.scheduler.initial-delay-ms:10000}",
            fixedDelayString = "${cockple.object-storage-delete-outbox.scheduler.fixed-delay-ms:60000}"
    )
    public void processPendingDeletes() {
        int processedCount = objectStorageDeleteOutboxProcessor.processPendingBatch();
        if (processedCount > 0) {
            log.info("Object storage 삭제 outbox 배치 처리 완료 - 처리 수: {}", processedCount);
        }
    }
}
