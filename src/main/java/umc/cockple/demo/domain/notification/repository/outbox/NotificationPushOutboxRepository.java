package umc.cockple.demo.domain.notification.repository.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.notification.domain.outbox.NotificationPushOutbox;
import umc.cockple.demo.domain.notification.enums.outbox.NotificationPushOutboxStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationPushOutboxRepository extends JpaRepository<NotificationPushOutbox, Long> {

    @Query("""
            SELECT outbox.id FROM NotificationPushOutbox outbox
            WHERE outbox.retryCount < :maxRetryCount
            AND (
                outbox.status IN :retryableStatuses
                OR (
                    outbox.status = :processingStatus
                    AND (outbox.lastAttemptedAt IS NULL OR outbox.lastAttemptedAt < :timeoutBefore)
                )
            )
            ORDER BY outbox.createdAt ASC
            """)
    List<Long> findClaimCandidateIds(
            @Param("retryableStatuses") Collection<NotificationPushOutboxStatus> retryableStatuses,
            @Param("processingStatus") NotificationPushOutboxStatus processingStatus,
            @Param("maxRetryCount") int maxRetryCount,
            @Param("timeoutBefore") LocalDateTime timeoutBefore,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE NotificationPushOutbox outbox
            SET outbox.status = :processingStatus,
                outbox.lastAttemptedAt = :claimedAt,
                outbox.claimToken = :claimToken
            WHERE outbox.id = :outboxId
            AND outbox.retryCount < :maxRetryCount
            AND (
                outbox.status IN :retryableStatuses
                OR (
                    outbox.status = :processingStatus
                    AND (outbox.lastAttemptedAt IS NULL OR outbox.lastAttemptedAt < :timeoutBefore)
                )
            )
            """)
    int claimForProcessing(
            @Param("outboxId") Long outboxId,
            @Param("retryableStatuses") Collection<NotificationPushOutboxStatus> retryableStatuses,
            @Param("processingStatus") NotificationPushOutboxStatus processingStatus,
            @Param("maxRetryCount") int maxRetryCount,
            @Param("timeoutBefore") LocalDateTime timeoutBefore,
            @Param("claimedAt") LocalDateTime claimedAt,
            @Param("claimToken") String claimToken
    );

    Optional<NotificationPushOutbox> findByIdAndStatusAndClaimToken(
            Long id,
            NotificationPushOutboxStatus status,
            String claimToken
    );
}
