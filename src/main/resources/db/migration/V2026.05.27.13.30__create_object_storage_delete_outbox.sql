CREATE TABLE IF NOT EXISTS object_storage_delete_outbox
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    created_at        DATETIME(6),
    updated_at        DATETIME(6),
    object_key        VARCHAR(512) NOT NULL,
    source_type       VARCHAR(50)  NOT NULL,
    source_id         BIGINT,
    PRIMARY KEY (id),
    INDEX idx_object_storage_delete_outbox_source (source_type, source_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
