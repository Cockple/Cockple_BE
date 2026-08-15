CREATE TABLE IF NOT EXISTS notification_push_outbox
(
    id                   BIGINT      NOT NULL AUTO_INCREMENT,
    created_at           DATETIME(6),
    updated_at           DATETIME(6),
    notification_id      BIGINT      NOT NULL,
    channel              VARCHAR(20) NOT NULL,
    status               VARCHAR(20) NOT NULL,
    retry_count          INT         NOT NULL DEFAULT 0,
    last_error           VARCHAR(2000),
    last_attempted_at    DATETIME(6),
    claim_token          VARCHAR(36),
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_push_outbox_notification_channel (notification_id, channel),
    INDEX idx_notification_push_outbox_status_created (status, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
