package umc.cockple.demo.domain.notification.service.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.notification.domain.outbox.NotificationOutbox;
import umc.cockple.demo.domain.notification.enums.outbox.NotificationOutboxStatus;
import umc.cockple.demo.domain.notification.repository.outbox.NotificationOutboxRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationOutboxClaimService {

    private final NotificationOutboxRepository notificationOutboxRepository;

    public Optional<ClaimedNotificationOutbox> claim(
            Long outboxId,
            int maxRetryCount,
            LocalDateTime processingTimeoutBefore
    ) {
        String claimToken = UUID.randomUUID().toString();
        int updatedCount = notificationOutboxRepository.claimForProcessing(
                outboxId,
                NotificationOutboxStatus.retryableStatuses(),
                NotificationOutboxStatus.PROCESSING,
                maxRetryCount,
                processingTimeoutBefore,
                LocalDateTime.now(),
                claimToken
        );

        if (updatedCount == 0) {
            return Optional.empty();
        }

        return notificationOutboxRepository
                .findByIdAndStatusAndClaimToken(outboxId, NotificationOutboxStatus.PROCESSING, claimToken)
                .map(outbox -> new ClaimedNotificationOutbox(outbox, claimToken));
    }

    public boolean markDone(ClaimedNotificationOutbox claimedOutbox) {
        return findClaimedOutbox(claimedOutbox)
                .map(outbox -> {
                    outbox.markDone();
                    return true;
                })
                .orElse(false);
    }

    public boolean markFailed(ClaimedNotificationOutbox claimedOutbox, String errorMessage) {
        return findClaimedOutbox(claimedOutbox)
                .map(outbox -> {
                    outbox.markFailed(errorMessage);
                    return true;
                })
                .orElse(false);
    }

    private Optional<NotificationOutbox> findClaimedOutbox(ClaimedNotificationOutbox claimedOutbox) {
        return notificationOutboxRepository.findByIdAndStatusAndClaimToken(
                claimedOutbox.outbox().getId(),
                NotificationOutboxStatus.PROCESSING,
                claimedOutbox.claimToken()
        );
    }
}
