package umc.cockple.demo.domain.notification.repository.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.notification.domain.outbox.NotificationOutbox;
import umc.cockple.demo.domain.notification.enums.outbox.NotificationOutboxStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

    @Query("""
            SELECT outbox.id FROM NotificationOutbox outbox
            WHERE outbox.retryCount < :maxRetryCount
            AND (
                outbox.status IN :retryableStatuses
                OR (
                    outbox.status = :processingStatus
                    AND (
                        outbox.lastAttemptedAt IS NULL
                        OR outbox.lastAttemptedAt < :processingTimeoutBefore
                    )
                )
            )
            ORDER BY outbox.createdAt ASC
            """)
    List<Long> findClaimCandidateIds(
            @Param("retryableStatuses") Collection<NotificationOutboxStatus> retryableStatuses,
            @Param("processingStatus") NotificationOutboxStatus processingStatus,
            @Param("maxRetryCount") int maxRetryCount,
            @Param("processingTimeoutBefore") LocalDateTime processingTimeoutBefore,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE NotificationOutbox outbox
            SET outbox.status = :processingStatus,
                outbox.lastAttemptedAt = :claimedAt,
                outbox.claimToken = :claimToken
            WHERE outbox.id = :outboxId
            AND outbox.retryCount < :maxRetryCount
            AND (
                outbox.status IN :retryableStatuses
                OR (
                    outbox.status = :processingStatus
                    AND (
                        outbox.lastAttemptedAt IS NULL
                        OR outbox.lastAttemptedAt < :processingTimeoutBefore
                    )
                )
            )
            """)
    int claimForProcessing(
            @Param("outboxId") Long outboxId,
            @Param("retryableStatuses") Collection<NotificationOutboxStatus> retryableStatuses,
            @Param("processingStatus") NotificationOutboxStatus processingStatus,
            @Param("maxRetryCount") int maxRetryCount,
            @Param("processingTimeoutBefore") LocalDateTime processingTimeoutBefore,
            @Param("claimedAt") LocalDateTime claimedAt,
            @Param("claimToken") String claimToken
    );

    Optional<NotificationOutbox> findByIdAndStatusAndClaimToken(
            Long id,
            NotificationOutboxStatus status,
            String claimToken
    );
}
