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
@DisplayName("운동 게임 진행자 마이그레이션")
class ExerciseGameHostMigrationTest {

    private static final String PREVIOUS_SCHEMA_VERSION = "2026.08.15.00.00";
    private static final String TARGET_SCHEMA_VERSION = "2026.08.17.15.00";

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.36");

    @Test
    @DisplayName("기존 운동의 모임장을 게임 진행자로 연결하고 필수 관계를 보장한다")
    void backfillExerciseGameHost() throws Exception {
        migrateToPreviousSchema();

        long ownerId;
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            ownerId = insertMember(statement);
            long partyId = insertParty(statement, ownerId);
            insertExercise(statement, partyId);
            insertExercise(statement, partyId);
        }

        migrateToTargetSchema();

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            assertThat(queryLong(statement,
                    "SELECT COUNT(*) FROM exercise WHERE game_host_id IS NULL"))
                    .isZero();
            assertThat(queryLong(statement,
                    "SELECT COUNT(*) FROM exercise WHERE game_host_id = " + ownerId))
                    .isEqualTo(2);

            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO exercise (
                        party_id,
                        date,
                        start_time,
                        max_capacity,
                        party_guest_accept,
                        outside_guest_accept
                    ) VALUES (1, '2099-12-31', '10:00:00', 10, 1, 0)
                    """))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> statement.executeUpdate(
                    "UPDATE exercise SET game_host_id = 999999999 WHERE id = 1"))
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

    private long insertMember(Statement statement) throws SQLException {
        statement.executeUpdate("""
                INSERT INTO member (member_name, nickname, is_active, social_id, token_version)
                VALUES ('모임장', 'owner', 'ACTIVE', 990000001, 0)
                """);
        return queryLong(statement, "SELECT LAST_INSERT_ID()");
    }

    private long insertParty(Statement statement, long ownerId) throws SQLException {
        statement.executeUpdate("""
                INSERT INTO party (
                    party_name,
                    party_type,
                    owner_id,
                    min_birth_year,
                    max_birth_year,
                    price,
                    join_price,
                    designated_cock,
                    exercise_count,
                    activity_time,
                    status
                ) VALUES ('테스트 모임', 'MIX_DOUBLES', %d, 1990, 2005, 10000, 5000, '셰틀콕', 2, 'MORNING', 'ACTIVE')
                """.formatted(ownerId));
        return queryLong(statement, "SELECT LAST_INSERT_ID()");
    }

    private void insertExercise(Statement statement, long partyId) throws SQLException {
        statement.executeUpdate("""
                INSERT INTO exercise (
                    party_id,
                    date,
                    start_time,
                    max_capacity,
                    party_guest_accept,
                    outside_guest_accept
                ) VALUES (%d, '2099-12-31', '10:00:00', 10, 1, 0)
                """.formatted(partyId));
    }

    private long queryLong(Statement statement, String sql) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}
