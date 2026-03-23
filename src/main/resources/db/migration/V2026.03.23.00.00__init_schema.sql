-- =====================================================
-- V1__init_schema.sql
-- Cockple 초기 스키마 (엔티티 기반)
-- =====================================================

CREATE TABLE IF NOT EXISTS terms
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    created_at     DATETIME(6),
    updated_at     DATETIME(6),
    content        VARCHAR(255) NOT NULL,
    version        VARCHAR(255) NOT NULL,
    effective_date DATE         NOT NULL,
    in_active      BIT          NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS party_addr
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    addr1      VARCHAR(255) NOT NULL,
    addr2      VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_addr1_addr2 UNIQUE (addr1, addr2)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS member
(
    id            BIGINT NOT NULL AUTO_INCREMENT,
    created_at    DATETIME(6),
    updated_at    DATETIME(6),
    member_name   VARCHAR(255),
    gender        VARCHAR(255),
    birth         DATE,
    level         VARCHAR(255),
    nickname      VARCHAR(255),
    is_active     VARCHAR(255) DEFAULT 'ACTIVE',
    refresh_token VARCHAR(255),
    social_id     BIGINT NOT NULL,
    fcm_token     VARCHAR(255),
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS party
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    created_at       DATETIME(6),
    updated_at       DATETIME(6),
    party_name       VARCHAR(255) NOT NULL,
    party_addr_id    BIGINT,
    party_type       VARCHAR(255) NOT NULL,
    owner_id         BIGINT       NOT NULL,
    min_birth_year   INTEGER      NOT NULL,
    max_birth_year   INTEGER      NOT NULL,
    price            INTEGER      NOT NULL,
    join_price       INTEGER      NOT NULL,
    designated_cock  VARCHAR(255) NOT NULL,
    exercise_count   INTEGER      NOT NULL DEFAULT 0,
    content          VARCHAR(255),
    activity_time    VARCHAR(255) NOT NULL,
    status           VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_party_party_addr FOREIGN KEY (party_addr_id) REFERENCES party_addr (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS exercise_addr
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    created_at    DATETIME(6),
    updated_at    DATETIME(6),
    addr1         VARCHAR(255) NOT NULL,
    addr2         VARCHAR(255) NOT NULL,
    street_addr   VARCHAR(255) NOT NULL,
    building_name VARCHAR(255) NOT NULL,
    latitude      DOUBLE       NOT NULL,
    longitude     DOUBLE       NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS exercise
(
    id                   BIGINT  NOT NULL AUTO_INCREMENT,
    created_at           DATETIME(6),
    updated_at           DATETIME(6),
    addr_id              BIGINT,
    party_id             BIGINT,
    date                 DATE    NOT NULL,
    start_time           TIME(6) NOT NULL,
    end_time             TIME(6),
    max_capacity         INTEGER NOT NULL,
    party_guest_accept   BIT     NOT NULL,
    outside_guest_accept BIT     NOT NULL,
    notice               VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_exercise_exercise_addr FOREIGN KEY (addr_id) REFERENCES exercise_addr (id),
    CONSTRAINT fk_exercise_party FOREIGN KEY (party_id) REFERENCES party (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS party_active_day
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    party_id   BIGINT,
    active_day VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_party_active_day_party FOREIGN KEY (party_id) REFERENCES party (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS party_img
(
    id       BIGINT NOT NULL AUTO_INCREMENT,
    party_id BIGINT,
    img_key  VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_party_img_party FOREIGN KEY (party_id) REFERENCES party (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS party_keyword
(
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    party_id BIGINT,
    keyword  VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_party_keyword_party FOREIGN KEY (party_id) REFERENCES party (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS party_level
(
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    party_id BIGINT,
    gender   VARCHAR(255) NOT NULL,
    level    VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_party_level_party FOREIGN KEY (party_id) REFERENCES party (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS member_addr
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    created_at    DATETIME(6),
    updated_at    DATETIME(6),
    member_id     BIGINT,
    addr1         VARCHAR(255) NOT NULL,
    addr2         VARCHAR(255) NOT NULL,
    addr3         VARCHAR(255) NOT NULL,
    street_addr   VARCHAR(255) NOT NULL,
    building_name VARCHAR(255),
    latitude      DOUBLE       NOT NULL,
    longitude     DOUBLE       NOT NULL,
    is_main       BIT          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_member_addr_member FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS member_exercise
(
    id                          BIGINT       NOT NULL AUTO_INCREMENT,
    created_at                  DATETIME(6),
    updated_at                  DATETIME(6),
    member_id                   BIGINT,
    exercise_id                 BIGINT,
    exercise_member_ship_status VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_member_exercise_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_member_exercise_exercise FOREIGN KEY (exercise_id) REFERENCES exercise (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS member_keyword
(
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    member_id BIGINT,
    keyword   VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_member_keyword_member FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS member_party
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    party_id   BIGINT,
    member_id  BIGINT,
    role       VARCHAR(255) NOT NULL,
    joined_at  DATETIME(6)  NOT NULL,
    status     VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_member_party_party FOREIGN KEY (party_id) REFERENCES party (id),
    CONSTRAINT fk_member_party_member FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS member_terms
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    member_id  BIGINT,
    terms_id   BIGINT,
    agree      BIT         NOT NULL,
    agreed_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_member_terms_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_member_terms_terms FOREIGN KEY (terms_id) REFERENCES terms (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS profile_img
(
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    member_id BIGINT,
    img_key   VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_profile_img_member FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS contest
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    created_at      DATETIME(6),
    updated_at      DATETIME(6),
    member_id       BIGINT,
    contest_name    VARCHAR(255) NOT NULL,
    medal_type      VARCHAR(255) NOT NULL,
    date            DATE,
    type            VARCHAR(255) NOT NULL,
    level           VARCHAR(255) NOT NULL,
    content         VARCHAR(255),
    content_is_open BIT          NOT NULL,
    video_is_open   BIT          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_contest_member FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS contest_img
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    contest_id BIGINT,
    img_key    VARCHAR(255) NOT NULL,
    img_order  INTEGER      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_contest_img_contest FOREIGN KEY (contest_id) REFERENCES contest (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS contest_video
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    contest_id  BIGINT,
    video_url   VARCHAR(255) NOT NULL,
    video_order INTEGER      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_contest_video_contest FOREIGN KEY (contest_id) REFERENCES contest (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notification
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    member_id  BIGINT,
    party_id   BIGINT       NOT NULL,
    title      VARCHAR(255),
    content    VARCHAR(255) NOT NULL,
    type       VARCHAR(255) NOT NULL,
    is_read    BIT          NOT NULL,
    image_key  VARCHAR(255),
    data       TEXT,
    PRIMARY KEY (id),
    CONSTRAINT fk_notification_member FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS exercise_bookmark
(
    id          BIGINT NOT NULL AUTO_INCREMENT,
    created_at  DATETIME(6),
    updated_at  DATETIME(6),
    member_id   BIGINT,
    exercise_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_exercise_bookmark_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_exercise_bookmark_exercise FOREIGN KEY (exercise_id) REFERENCES exercise (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS party_bookmark
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    member_id  BIGINT,
    party_id   BIGINT,
    order_type VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_party_bookmark_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_party_bookmark_party FOREIGN KEY (party_id) REFERENCES party (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS party_invitation
(
    id         BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    party_id   BIGINT,
    inviter_id BIGINT,
    invitee_id BIGINT,
    status     VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_party_invitation_party FOREIGN KEY (party_id) REFERENCES party (id),
    CONSTRAINT fk_party_invitation_inviter FOREIGN KEY (inviter_id) REFERENCES member (id),
    CONSTRAINT fk_party_invitation_invitee FOREIGN KEY (invitee_id) REFERENCES member (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS party_join_request
(
    id         BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    member_id  BIGINT,
    party_id   BIGINT,
    status     VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_party_join_request_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_party_join_request_party FOREIGN KEY (party_id) REFERENCES party (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS guest
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    exercise_id BIGINT,
    guest_name VARCHAR(255) NOT NULL,
    gender     VARCHAR(255) NOT NULL,
    level      VARCHAR(255) NOT NULL,
    inviter_id BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_guest_exercise FOREIGN KEY (exercise_id) REFERENCES exercise (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_room
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    party_id   BIGINT,
    type       VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_chat_room_party FOREIGN KEY (party_id) REFERENCES party (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_room_member
(
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    created_at          DATETIME(6),
    updated_at          DATETIME(6),
    chat_room_id        BIGINT,
    member_id           BIGINT,
    display_name        VARCHAR(255),
    last_read_message_id BIGINT,
    status              VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_chat_room_member_chat_room FOREIGN KEY (chat_room_id) REFERENCES chat_room (id),
    CONSTRAINT fk_chat_room_member_member FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_message
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    created_at   DATETIME(6),
    updated_at   DATETIME(6),
    chat_room_id BIGINT,
    sender_id    BIGINT,
    content      TEXT,
    type         VARCHAR(255) NOT NULL,
    is_deleted   BIT          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_chat_message_chat_room FOREIGN KEY (chat_room_id) REFERENCES chat_room (id),
    CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_id) REFERENCES member (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_message_file
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    created_at         DATETIME(6),
    updated_at         DATETIME(6),
    chat_message_id    BIGINT,
    file_key           VARCHAR(255) NOT NULL,
    file_order         INTEGER      NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_size          BIGINT,
    file_type          VARCHAR(255),
    is_emoji           BIT          NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_chat_message_file_chat_message FOREIGN KEY (chat_message_id) REFERENCES chat_message (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS message_read_status
(
    id              BIGINT  NOT NULL AUTO_INCREMENT,
    chat_message_id BIGINT  NOT NULL,
    member_id       BIGINT  NOT NULL,
    chat_room_id    BIGINT  NOT NULL,
    is_read         BIT     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_message_read_status_message_member UNIQUE (chat_message_id, member_id),
    INDEX idx_message_read_message (chat_message_id),
    INDEX idx_message_read_member (member_id),
    INDEX idx_message_read_chatroom_member (chat_room_id, member_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS download_token
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    token      VARCHAR(255) NOT NULL UNIQUE,
    file_id    BIGINT       NOT NULL,
    member_id  BIGINT       NOT NULL,
    expires_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
