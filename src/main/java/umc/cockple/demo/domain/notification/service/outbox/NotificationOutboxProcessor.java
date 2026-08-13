package umc.cockple.demo.domain.notification.service.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import umc.cockple.demo.domain.notification.domain.outbox.NotificationOutbox;
import umc.cockple.demo.domain.notification.enums.outbox.NotificationOutboxStatus;
import umc.cockple.demo.domain.notification.repository.outbox.NotificationOutboxRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationOutboxProcessor {

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final NotificationOutboxClaimService notificationOutboxClaimService;
    private final List<NotificationOutboxHandler> handlers;

    @Value("${cockple.notification-outbox.batch-size:50}")
    private int batchSize;

    @Value("${cockple.notification-outbox.max-retry-count:5}")
    private int maxRetryCount;

    @Value("${cockple.notification-outbox.processing-timeout-minutes:10}")
    private long processingTimeoutMinutes;

    public int processPendingBatch() {
        LocalDateTime processingTimeoutBefore = LocalDateTime.now().minusMinutes(processingTimeoutMinutes);
        List<Long> outboxIds = notificationOutboxRepository.findClaimCandidateIds(
                NotificationOutboxStatus.retryableStatuses(),
                NotificationOutboxStatus.PROCESSING,
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
        Optional<ClaimedNotificationOutbox> claimedOutbox = notificationOutboxClaimService.claim(
                outboxId,
                maxRetryCount,
                processingTimeoutBefore
        );

        return claimedOutbox.map(this::process).orElse(false);
    }

    private boolean process(ClaimedNotificationOutbox claimedOutbox) {
        NotificationOutbox outbox = claimedOutbox.outbox();
        Optional<NotificationOutboxHandler> handler = handlers.stream()
                .filter(candidate -> candidate.supports(outbox))
                .findFirst();

        if (handler.isEmpty()) {
            log.warn("Notification outbox 처리 핸들러가 없습니다 - outboxId: {}, eventType: {}",
                    outbox.getId(), outbox.getEventType());
            notificationOutboxClaimService.markDead(claimedOutbox, "처리 핸들러를 찾을 수 없습니다.");
            return true;
        }

        try {
            handler.get().handle(outbox);
            return notificationOutboxClaimService.markDone(claimedOutbox);
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException
                    || e instanceof com.fasterxml.jackson.core.JsonProcessingException) {
                notificationOutboxClaimService.markDead(claimedOutbox, e.getMessage());
            } else {
                notificationOutboxClaimService.markFailed(claimedOutbox, e.getMessage(), maxRetryCount);
            }
            log.warn("Notification outbox 처리 실패 - outboxId: {}", outbox.getId(), e);
            return true;
        }
    }
}
