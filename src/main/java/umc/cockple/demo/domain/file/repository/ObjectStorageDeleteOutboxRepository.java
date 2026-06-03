package umc.cockple.demo.domain.file.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.file.domain.ObjectStorageDeleteOutbox;
import umc.cockple.demo.domain.file.enums.ObjectStorageDeleteStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ObjectStorageDeleteOutboxRepository extends JpaRepository<ObjectStorageDeleteOutbox, Long> {

    @Query("""
            SELECT outbox.id FROM ObjectStorageDeleteOutbox outbox
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
            @Param("retryableStatuses") Collection<ObjectStorageDeleteStatus> retryableStatuses,
            @Param("processingStatus") ObjectStorageDeleteStatus processingStatus,
            @Param("maxRetryCount") int maxRetryCount,
            @Param("processingTimeoutBefore") LocalDateTime processingTimeoutBefore,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ObjectStorageDeleteOutbox outbox
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
            @Param("retryableStatuses") Collection<ObjectStorageDeleteStatus> retryableStatuses,
            @Param("processingStatus") ObjectStorageDeleteStatus processingStatus,
            @Param("maxRetryCount") int maxRetryCount,
            @Param("processingTimeoutBefore") LocalDateTime processingTimeoutBefore,
            @Param("claimedAt") LocalDateTime claimedAt,
            @Param("claimToken") String claimToken
    );

    Optional<ObjectStorageDeleteOutbox> findByIdAndStatusAndClaimToken(
            Long id,
            ObjectStorageDeleteStatus status,
            String claimToken
    );
}
