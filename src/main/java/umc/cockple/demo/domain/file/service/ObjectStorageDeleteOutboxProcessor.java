package umc.cockple.demo.domain.file.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import umc.cockple.demo.domain.file.enums.ObjectStorageDeleteStatus;
import umc.cockple.demo.domain.file.repository.ObjectStorageDeleteOutboxRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ObjectStorageDeleteOutboxProcessor {

    private final ObjectStorageDeleteOutboxRepository objectStorageDeleteOutboxRepository;
    private final ObjectStorageDeleteOutboxClaimService objectStorageDeleteOutboxClaimService;
    private final ObjectStorageClient objectStorageClient;

    @Value("${cockple.object-storage-delete-outbox.batch-size:50}")
    private int batchSize;

    @Value("${cockple.object-storage-delete-outbox.max-retry-count:5}")
    private int maxRetryCount;

    @Value("${cockple.object-storage-delete-outbox.processing-timeout-minutes:10}")
    private long processingTimeoutMinutes;

    public int processPendingBatch() {
        LocalDateTime processingTimeoutBefore = LocalDateTime.now().minusMinutes(processingTimeoutMinutes);
        List<Long> outboxIds = objectStorageDeleteOutboxRepository
                .findClaimCandidateIds(
                        ObjectStorageDeleteStatus.retryableStatuses(),
                        ObjectStorageDeleteStatus.PROCESSING,
                        maxRetryCount,
                        processingTimeoutBefore,
                        PageRequest.of(0, batchSize)
                );

        int processedCount = 0;
        for (Long outboxId : outboxIds) {
            if (processOne(outboxId)) {
                processedCount++;
            }
        }
        return processedCount;
    }

    public boolean processOne(Long outboxId) {
        LocalDateTime processingTimeoutBefore = LocalDateTime.now().minusMinutes(processingTimeoutMinutes);
        Optional<ClaimedObjectStorageDeleteOutbox> claimedOutbox = objectStorageDeleteOutboxClaimService.claim(
                outboxId,
                maxRetryCount,
                processingTimeoutBefore
        );

        return claimedOutbox
                .map(this::process)
                .orElse(false);
    }

    private boolean process(ClaimedObjectStorageDeleteOutbox outbox) {
        try {
            objectStorageClient.delete(outbox.objectKey());
            if (objectStorageDeleteOutboxClaimService.markDone(outbox)) {
                log.info("Object storage 삭제 outbox 처리 완료 - outboxId: {}, objectKey: {}", outbox.id(), outbox.objectKey());
            } else {
                log.warn("Object storage 삭제 outbox 처리 완료 후 상태 변경 스킵 - outboxId: {}, objectKey: {}", outbox.id(), outbox.objectKey());
            }
        } catch (Exception e) {
            if (objectStorageDeleteOutboxClaimService.markFailed(outbox, e.getMessage())) {
                log.warn("Object storage 삭제 outbox 처리 실패 - outboxId: {}, objectKey: {}", outbox.id(), outbox.objectKey(), e);
            } else {
                log.warn("Object storage 삭제 outbox 처리 실패 후 상태 변경 스킵 - outboxId: {}, objectKey: {}", outbox.id(), outbox.objectKey(), e);
            }
        }
        return true;
    }
}
