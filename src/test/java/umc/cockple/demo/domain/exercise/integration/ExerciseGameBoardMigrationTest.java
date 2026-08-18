package umc.cockple.demo.domain.exercise.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("integration")
@Testcontainers
@DisplayName("운동 게임판 백필 마이그레이션")
class ExerciseGameBoardMigrationTest {

    private static final String PREVIOUS_SCHEMA_VERSION = "2026.08.15.00.00";
    private static final String TARGET_SCHEMA_VERSION = "2026.08.17.14.00";

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.36");

    @Test
    @DisplayName("기존 운동에 게임판을 연결하고 필수 1:1 관계를 보장한다")
    void backfillExerciseGameBoards() throws Exception {
        migrateToPreviousSchema();

        long existingGameBoardId;
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO game_board (created_at, updated_at) VALUES (NOW(6), NOW(6))");
            existingGameBoardId = generatedId(statement, "SELECT LAST_INSERT_ID()");

            insertExercise(statement, existingGameBoardId);
            insertExercise(statement, null);
            insertExercise(statement, null);
        }

        migrateToTargetSchema();

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            assertThat(queryLong(statement,
                    "SELECT COUNT(*) FROM exercise WHERE game_board_id IS NULL"))
                    .isZero();
            assertThat(queryLong(statement,
                    "SELECT COUNT(DISTINCT game_board_id) FROM exercise"))
                    .isEqualTo(3);
            assertThat(queryLong(statement,
                    "SELECT COUNT(*) FROM game_board"))
                    .isEqualTo(3);
            assertThat(queryLong(statement,
                    "SELECT game_board_id FROM exercise ORDER BY id LIMIT 1"))
                    .isEqualTo(existingGameBoardId);

            assertThatThrownBy(() -> insertExercise(statement, null))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> statement.executeUpdate(
                    "UPDATE exercise SET game_board_id = " + existingGameBoardId + " WHERE id = 2"))
                    .isInstanceOf(SQLException.class);
        }
    }

    private void migrateToPreviousSchema() {
        flyway().target(PREVIOUS_SCHEMA_VERSION).load().migrate();
    }

    private void migrateToTargetSchema() {
        flyway().target(TARGET_SCHEMA_VERSION).load().migrate();
    }

    private org.flywaydb.core.api.configuration.FluentConfiguration flyway() {
        return Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .locations("classpath:db/migration");
    }

    private Connection connection() throws SQLException {
        return mysql.createConnection("");
    }

    private void insertExercise(Statement statement, Long gameBoardId) throws SQLException {
        String gameBoardValue = gameBoardId == null ? "NULL" : gameBoardId.toString();
        statement.executeUpdate("""
                INSERT INTO exercise (
                    date,
                    start_time,
                    max_capacity,
                    party_guest_accept,
                    outside_guest_accept,
                    game_board_id
                ) VALUES ('2099-12-31', '10:00:00', 10, 1, 0, %s)
                """.formatted(gameBoardValue));
    }

    private long generatedId(Statement statement, String sql) throws SQLException {
        return queryLong(statement, sql);
    }

    private long queryLong(Statement statement, String sql) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}
