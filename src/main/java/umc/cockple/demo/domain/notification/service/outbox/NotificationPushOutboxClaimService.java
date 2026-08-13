package umc.cockple.demo.domain.notification.service.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.notification.domain.outbox.NotificationPushOutbox;
import umc.cockple.demo.domain.notification.enums.outbox.NotificationPushOutboxStatus;
import umc.cockple.demo.domain.notification.repository.outbox.NotificationPushOutboxRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationPushOutboxClaimService {

    private final NotificationPushOutboxRepository repository;

    public Optional<ClaimedNotificationPushOutbox> claim(Long outboxId, int maxRetryCount,
                                                          LocalDateTime timeoutBefore) {
        String claimToken = UUID.randomUUID().toString();
        int updatedCount = repository.claimForProcessing(
                outboxId,
                NotificationPushOutboxStatus.retryableStatuses(),
                NotificationPushOutboxStatus.PROCESSING,
                maxRetryCount,
                timeoutBefore,
                LocalDateTime.now(),
                claimToken
        );

        if (updatedCount == 0) {
            return Optional.empty();
        }

        return repository.findByIdAndStatusAndClaimToken(
                        outboxId, NotificationPushOutboxStatus.PROCESSING, claimToken)
                .map(outbox -> new ClaimedNotificationPushOutbox(outbox, claimToken));
    }

    public boolean markDone(ClaimedNotificationPushOutbox claimed) {
        return findClaimed(claimed).map(outbox -> {
            outbox.markDone();
            return true;
        }).orElse(false);
    }

    public boolean markFailed(ClaimedNotificationPushOutbox claimed, String errorMessage) {
        return findClaimed(claimed).map(outbox -> {
            outbox.markFailed(errorMessage);
            return true;
        }).orElse(false);
    }

    private Optional<NotificationPushOutbox> findClaimed(ClaimedNotificationPushOutbox claimed) {
        return repository.findByIdAndStatusAndClaimToken(
                claimed.outbox().getId(), NotificationPushOutboxStatus.PROCESSING, claimed.claimToken());
    }
}
