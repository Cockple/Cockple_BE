package umc.cockple.demo.domain.file.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.file.domain.ObjectStorageDeleteOutbox;
import umc.cockple.demo.domain.file.enums.ObjectStorageDeleteStatus;
import umc.cockple.demo.domain.file.repository.ObjectStorageDeleteOutboxRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ObjectStorageDeleteOutboxProcessor {

    private final ObjectStorageDeleteOutboxRepository objectStorageDeleteOutboxRepository;
    private final ObjectStorageClient objectStorageClient;

    @Value("${cockple.object-storage-delete-outbox.batch-size:50}")
    private int batchSize;

    @Value("${cockple.object-storage-delete-outbox.max-retry-count:5}")
    private int maxRetryCount;

    @Transactional
    public int processPendingBatch() {
        List<ObjectStorageDeleteOutbox> outboxes = objectStorageDeleteOutboxRepository
                .findByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(
                        List.of(ObjectStorageDeleteStatus.PENDING, ObjectStorageDeleteStatus.FAILED),
                        maxRetryCount,
                        PageRequest.of(0, batchSize)
                );

        outboxes.forEach(this::process);
        return outboxes.size();
    }

    @Transactional
    public void processOne(Long outboxId) {
        ObjectStorageDeleteOutbox outbox = objectStorageDeleteOutboxRepository.findById(outboxId)
                .orElseThrow(() -> new IllegalArgumentException("Object storage 삭제 outbox를 찾을 수 없습니다. id=" + outboxId));

        process(outbox);
    }

    private void process(ObjectStorageDeleteOutbox outbox) {
        try {
            objectStorageClient.delete(outbox.getObjectKey());
            outbox.markDone();
            log.info("Object storage 삭제 outbox 처리 완료 - outboxId: {}, objectKey: {}", outbox.getId(), outbox.getObjectKey());
        } catch (Exception e) {
            outbox.markFailed(e.getMessage());
            log.warn(
                    "Object storage 삭제 outbox 처리 실패 - outboxId: {}, objectKey: {}, retryCount: {}",
                    outbox.getId(),
                    outbox.getObjectKey(),
                    outbox.getRetryCount(),
                    e
            );
        }
    }
}
