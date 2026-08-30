CREATE TABLE IF NOT EXISTS notification_outbox
(
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    created_at           DATETIME(6),
    updated_at           DATETIME(6),
    event_id             CHAR(36)     NOT NULL,
    event_type           VARCHAR(50)  NOT NULL,
    source               VARCHAR(30)  NOT NULL,
    recipient_member_id  BIGINT       NOT NULL,
    title                TEXT         NOT NULL,
    content              TEXT         NOT NULL,
    image_key            VARCHAR(512),
    data                 TEXT,
    resource_type        VARCHAR(50),
    resource_id          BIGINT,
    action               VARCHAR(30),
    legacy_party_id      BIGINT,
    legacy_type          VARCHAR(30),
    notification_key     VARCHAR(255) NOT NULL,
    status               VARCHAR(20)  NOT NULL,
    retry_count          INT          NOT NULL DEFAULT 0,
    last_error           VARCHAR(2000),
    last_attempted_at    DATETIME(6),
    claim_token          VARCHAR(36),
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_outbox_notification_key (notification_key),
    INDEX idx_notification_outbox_status_created (status, created_at),
    INDEX idx_notification_outbox_event (event_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
