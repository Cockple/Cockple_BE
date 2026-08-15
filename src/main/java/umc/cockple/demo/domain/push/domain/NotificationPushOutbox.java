package umc.cockple.demo.domain.push.domain;

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
import umc.cockple.demo.domain.push.command.NotificationPushOutboxPayload;
import umc.cockple.demo.domain.push.enums.NotificationPushChannel;
import umc.cockple.demo.domain.push.enums.NotificationPushOutboxStatus;
import umc.cockple.demo.domain.push.enums.NotificationPushTargetType;
import umc.cockple.demo.domain.chat.enums.ChatRoomType;
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

    @Column(name = "notification_id")
    private Long notificationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private NotificationPushTargetType targetType;

    @Column(name = "chat_room_id")
    private Long chatRoomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "chat_room_type", length = 30)
    private ChatRoomType chatRoomType;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "sender_id")
    private Long senderId;

    @Column(name = "active_subscriber_ids", columnDefinition = "TEXT")
    private String activeSubscriberIds;

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

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "claim_token", length = 36)
    private String claimToken;

    /**
     * 동일 이벤트의 중복 적재를 막기 위한 키다.
     * 외부 FCM 호출 성공 후 DONE 저장 전에 프로세스가 종료되면 재발송될 수 있어
     * exactly-once 발송까지 보장하지는 않는다.
     */
    @Column(name = "deduplication_key", nullable = false, length = 255)
    private String deduplicationKey;

    public static NotificationPushOutbox pending(NotificationPushOutboxPayload payload) {
        return NotificationPushOutbox.builder()
                .notificationId(payload.notificationId())
                .targetType(payload.targetType())
                .chatRoomId(payload.chatRoomId())
                .chatRoomType(payload.chatRoomType())
                .title(payload.title())
                .content(payload.content())
                .senderId(payload.senderId())
                .activeSubscriberIds(payload.activeSubscriberIdsJson())
                .deduplicationKey(payload.deduplicationKey())
                .channel(payload.channel())
                .status(NotificationPushOutboxStatus.PENDING)
                .retryCount(0)
                .build();
    }

    public void markProcessing(LocalDateTime attemptedAt, String claimToken) {
        this.status = NotificationPushOutboxStatus.PROCESSING;
        this.retryCount++;
        this.lastAttemptedAt = attemptedAt;
        this.claimToken = claimToken;
    }

    public void markDone() {
        this.status = NotificationPushOutboxStatus.DONE;
        this.lastError = null;
        this.lastAttemptedAt = LocalDateTime.now();
        this.nextAttemptAt = null;
        this.claimToken = null;
    }

    public void markFailed(String errorMessage) {
        this.status = NotificationPushOutboxStatus.FAILED;
        this.lastError = truncate(errorMessage);
        this.lastAttemptedAt = LocalDateTime.now();
        this.nextAttemptAt = LocalDateTime.now().plusSeconds(backoffSeconds());
        this.claimToken = null;
    }

    public void markDead(String errorMessage) {
        this.status = NotificationPushOutboxStatus.DEAD;
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
