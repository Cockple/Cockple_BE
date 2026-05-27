package umc.cockple.demo.domain.file.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.file.domain.ObjectStorageDeleteOutbox;
import umc.cockple.demo.domain.file.enums.ObjectStorageDeleteStatus;
import umc.cockple.demo.domain.file.repository.ObjectStorageDeleteOutboxRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ObjectStorageDeleteOutboxClaimService {

    private final ObjectStorageDeleteOutboxRepository objectStorageDeleteOutboxRepository;

    @Transactional
    public Optional<ClaimedObjectStorageDeleteOutbox> claim(
            Long outboxId,
            int maxRetryCount,
            LocalDateTime processingTimeoutBefore
    ) {
        String claimToken = UUID.randomUUID().toString();
        int updatedCount = objectStorageDeleteOutboxRepository.claimForProcessing(
                outboxId,
                ObjectStorageDeleteStatus.retryableStatuses(),
                ObjectStorageDeleteStatus.PROCESSING,
                maxRetryCount,
                processingTimeoutBefore,
                LocalDateTime.now(),
                claimToken
        );

        if (updatedCount == 0) {
            if (!objectStorageDeleteOutboxRepository.existsById(outboxId)) {
                throw new IllegalArgumentException("Object storage 삭제 outbox를 찾을 수 없습니다. id=" + outboxId);
            }
            return Optional.empty();
        }

        return objectStorageDeleteOutboxRepository
                .findByIdAndStatusAndClaimToken(outboxId, ObjectStorageDeleteStatus.PROCESSING, claimToken)
                .map(outbox -> new ClaimedObjectStorageDeleteOutbox(
                        outbox.getId(),
                        outbox.getObjectKey(),
                        claimToken
                ));
    }

    @Transactional
    public boolean markDone(ClaimedObjectStorageDeleteOutbox claimedOutbox) {
        return findClaimedOutbox(claimedOutbox)
                .map(outbox -> {
                    outbox.markDone();
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public boolean markFailed(ClaimedObjectStorageDeleteOutbox claimedOutbox, String errorMessage) {
        return findClaimedOutbox(claimedOutbox)
                .map(outbox -> {
                    outbox.markFailed(errorMessage);
                    return true;
                })
                .orElse(false);
    }

    private Optional<ObjectStorageDeleteOutbox> findClaimedOutbox(ClaimedObjectStorageDeleteOutbox claimedOutbox) {
        return objectStorageDeleteOutboxRepository.findByIdAndStatusAndClaimToken(
                claimedOutbox.id(),
                ObjectStorageDeleteStatus.PROCESSING,
                claimedOutbox.claimToken()
        );
    }
}
