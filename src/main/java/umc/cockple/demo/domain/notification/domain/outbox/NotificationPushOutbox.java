package umc.cockple.demo.domain.notification.domain.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import umc.cockple.demo.domain.notification.command.outbox.NotificationPushOutboxPayload;
import umc.cockple.demo.domain.notification.enums.outbox.NotificationPushChannel;
import umc.cockple.demo.domain.notification.enums.outbox.NotificationPushOutboxStatus;
import umc.cockple.demo.global.common.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_push_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NotificationPushOutbox extends BaseEntity {

    private static final int LAST_ERROR_MAX_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationPushChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationPushOutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error", length = LAST_ERROR_MAX_LENGTH)
    private String lastError;

    @Column(name = "last_attempted_at")
    private LocalDateTime lastAttemptedAt;

    @Column(name = "claim_token", length = 36)
    private String claimToken;

    public static NotificationPushOutbox pending(NotificationPushOutboxPayload payload) {
        return NotificationPushOutbox.builder()
                .notificationId(payload.notificationId())
                .channel(payload.channel())
                .status(NotificationPushOutboxStatus.PENDING)
                .retryCount(0)
                .build();
    }

    public void markDone() {
        this.status = NotificationPushOutboxStatus.DONE;
        this.lastError = null;
        this.lastAttemptedAt = LocalDateTime.now();
        this.claimToken = null;
    }

    public void markFailed(String errorMessage) {
        this.status = NotificationPushOutboxStatus.FAILED;
        this.retryCount++;
        this.lastError = truncate(errorMessage);
        this.lastAttemptedAt = LocalDateTime.now();
        this.claimToken = null;
    }

    private String truncate(String value) {
        if (value == null || value.length() <= LAST_ERROR_MAX_LENGTH) {
            return value;
        }
        return value.substring(0, LAST_ERROR_MAX_LENGTH);
    }
}
