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
import umc.cockple.demo.global.common.BaseEntity;

@Entity
@Table(name = "object_storage_delete_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ObjectStorageDeleteOutbox extends BaseEntity {

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

    public static ObjectStorageDeleteOutbox pending(String objectKey, ObjectStorageDeleteSourceType sourceType, Long sourceId) {
        return ObjectStorageDeleteOutbox.builder()
                .objectKey(objectKey)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .build();
    }
}
