package umc.cockple.demo.domain.notification.domain.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import umc.cockple.demo.domain.notification.command.outbox.NotificationOutboxPayload;
import umc.cockple.demo.domain.notification.domain.NotificationDestination;
import umc.cockple.demo.domain.notification.domain.NotificationLegacyCompatibility;
import umc.cockple.demo.domain.notification.enums.NotificationAction;
import umc.cockple.demo.domain.notification.enums.outbox.NotificationOutboxEventType;
import umc.cockple.demo.domain.notification.enums.outbox.NotificationOutboxStatus;
import umc.cockple.demo.domain.notification.enums.NotificationResourceType;
import umc.cockple.demo.domain.notification.enums.NotificationSource;
import umc.cockple.demo.domain.notification.enums.NotificationType;
import umc.cockple.demo.global.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

import static java.sql.Types.CHAR;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NotificationOutbox extends BaseEntity {

    private static final int LAST_ERROR_MAX_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(CHAR)
    @Column(name = "event_id", nullable = false, length = 36)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private NotificationOutboxEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationSource source;

    @Column(name = "recipient_member_id", nullable = false)
    private Long recipientMemberId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_key", length = 512)
    private String imageKey;

    @Column(columnDefinition = "TEXT")
    private String data;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", length = 50)
    private NotificationResourceType resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private NotificationAction action;

    @Column(name = "legacy_party_id")
    private Long legacyPartyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "legacy_type", length = 30)
    private NotificationType legacyType;

    @Column(name = "notification_key", nullable = false, length = 255)
    private String notificationKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationOutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error", length = LAST_ERROR_MAX_LENGTH)
    private String lastError;

    @Column(name = "last_attempted_at")
    private LocalDateTime lastAttemptedAt;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "claim_token", length = 36)
    private String claimToken;

    public static NotificationOutbox pending(NotificationOutboxPayload payload) {
        NotificationDestination destination = payload.destination();
        NotificationLegacyCompatibility legacyCompatibility = payload.legacyCompatibility();

        return NotificationOutbox.builder()
                .eventId(payload.eventId())
                .eventType(payload.eventType())
                .source(payload.source())
                .recipientMemberId(payload.recipientMemberId())
                .title(payload.title())
                .content(payload.content())
                .imageKey(payload.imageKey())
                .data(payload.data())
                .resourceType(destination == null ? null : destination.resourceType())
                .resourceId(destination == null ? null : destination.resourceId())
                .action(destination == null ? null : destination.action())
                .legacyPartyId(legacyCompatibility == null ? null : legacyCompatibility.partyId())
                .legacyType(legacyCompatibility == null ? null : legacyCompatibility.type())
                .notificationKey(payload.notificationKey())
                .status(NotificationOutboxStatus.PENDING)
                .retryCount(0)
                .build();
    }

    public void markProcessing(LocalDateTime attemptedAt, String claimToken) {
        this.status = NotificationOutboxStatus.PROCESSING;
        this.retryCount++;
        this.lastAttemptedAt = attemptedAt;
        this.claimToken = claimToken;
    }

    public void markDone() {
        this.status = NotificationOutboxStatus.DONE;
        this.lastError = null;
        this.lastAttemptedAt = LocalDateTime.now();
        this.nextAttemptAt = null;
        this.claimToken = null;
    }

    public void markFailed(String errorMessage) {
        this.status = NotificationOutboxStatus.FAILED;
        this.lastError = truncate(errorMessage);
        this.lastAttemptedAt = LocalDateTime.now();
        this.nextAttemptAt = LocalDateTime.now().plusSeconds(backoffSeconds());
        this.claimToken = null;
    }

    public void markDead(String errorMessage) {
        this.status = NotificationOutboxStatus.DEAD;
        this.lastError = truncate(errorMessage);
        this.lastAttemptedAt = LocalDateTime.now();
        this.nextAttemptAt = null;
        this.claimToken = null;
    }

    private long backoffSeconds() {
        return Math.min(3600L, 30L * (1L << Math.min(Math.max(retryCount - 1, 0), 7)));
    }

    private String truncate(String value) {
        if (value == null || value.length() <= LAST_ERROR_MAX_LENGTH) {
            return value;
        }
        return value.substring(0, LAST_ERROR_MAX_LENGTH);
    }

}
