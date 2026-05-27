package umc.cockple.demo.domain.file.domain;

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
import umc.cockple.demo.domain.file.enums.ObjectStorageDeleteSourceType;
import umc.cockple.demo.domain.file.enums.ObjectStorageDeleteStatus;
import umc.cockple.demo.global.common.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "object_storage_delete_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ObjectStorageDeleteOutbox extends BaseEntity {

    private static final int LAST_ERROR_MAX_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "object_key", nullable = false, length = 512)
    private String objectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private ObjectStorageDeleteSourceType sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ObjectStorageDeleteStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error", length = LAST_ERROR_MAX_LENGTH)
    private String lastError;

    @Column(name = "last_attempted_at")
    private LocalDateTime lastAttemptedAt;

    public static ObjectStorageDeleteOutbox pending(String objectKey, ObjectStorageDeleteSourceType sourceType, Long sourceId) {
        return ObjectStorageDeleteOutbox.builder()
                .objectKey(objectKey)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .status(ObjectStorageDeleteStatus.PENDING)
                .retryCount(0)
                .build();
    }

    public void markDone() {
        this.status = ObjectStorageDeleteStatus.DONE;
        this.lastError = null;
        this.lastAttemptedAt = LocalDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = ObjectStorageDeleteStatus.FAILED;
        this.retryCount++;
        this.lastError = truncate(errorMessage);
        this.lastAttemptedAt = LocalDateTime.now();
    }

    private String truncate(String value) {
        if (value == null || value.length() <= LAST_ERROR_MAX_LENGTH) {
            return value;
        }
        return value.substring(0, LAST_ERROR_MAX_LENGTH);
    }
}
