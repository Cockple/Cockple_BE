-- 게임판 도메인 스키마 생성
-- GameBoard(게임판) 애그리거트: 코트/게임/게임판멤버/플레이어 + Exercise 연결(FK)

CREATE TABLE IF NOT EXISTS game_board
(
    id         BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS court
(
    id            BIGINT  NOT NULL AUTO_INCREMENT,
    created_at    DATETIME(6),
    updated_at    DATETIME(6),
    game_board_id BIGINT,
    court_no      INTEGER NOT NULL,
    court_name    VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_court_game_board FOREIGN KEY (game_board_id) REFERENCES game_board (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS game_board_member
(
    id                    BIGINT      NOT NULL AUTO_INCREMENT,
    created_at            DATETIME(6),
    updated_at            DATETIME(6),
    game_board_id         BIGINT,
    name                  VARCHAR(255) NOT NULL,
    gender                VARCHAR(20)  NOT NULL,
    level                 VARCHAR(20)  NOT NULL,
    age_group             VARCHAR(20),
    shuttlecock_submitted BIT          NOT NULL,
    participating         BIT          NOT NULL,
    game_count            INTEGER      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_game_board_member_game_board FOREIGN KEY (game_board_id) REFERENCES game_board (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS game
(
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    created_at    DATETIME(6),
    updated_at    DATETIME(6),
    game_board_id BIGINT,
    court_id      BIGINT,
    status        VARCHAR(20) NOT NULL,
    waiting_order INTEGER,
    started_at    DATETIME(6),
    completed_at  DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_game_game_board FOREIGN KEY (game_board_id) REFERENCES game_board (id),
    CONSTRAINT fk_game_court FOREIGN KEY (court_id) REFERENCES court (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS game_player
(
    id                   BIGINT  NOT NULL AUTO_INCREMENT,
    created_at           DATETIME(6),
    updated_at           DATETIME(6),
    game_id              BIGINT,
    game_board_member_id BIGINT,
    player_order         INTEGER NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_game_player_game FOREIGN KEY (game_id) REFERENCES game (id),
    CONSTRAINT fk_game_player_game_board_member FOREIGN KEY (game_board_member_id) REFERENCES game_board_member (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- Exercise 1:1 GameBoard (Exercise가 FK 소유)
ALTER TABLE exercise
    ADD COLUMN game_board_id BIGINT;

ALTER TABLE exercise
    ADD CONSTRAINT fk_exercise_game_board FOREIGN KEY (game_board_id) REFERENCES game_board (id);
