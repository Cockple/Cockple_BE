package umc.cockple.demo.domain.game.integration;

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
@DisplayName("게임판 명단 백필 마이그레이션")
class GameBoardMemberBackfillMigrationTest {

    private static final String ROSTER_SOURCE_SCHEMA_VERSION = "2026.08.17.14.10";
    private static final String TARGET_SCHEMA_VERSION = "2026.08.17.14.20";

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.36");

    @Test
    @DisplayName("모든 기존 회원 참가자와 게스트를 중복 없이 게임판 명단으로 백필한다")
    void backfillGameBoardMembers() throws Exception {
        migrateToRosterSourceSchema();

        long firstBoardId;
        long secondBoardId;
        long firstExerciseId;
        long secondExerciseId;
        long firstMemberId;
        long existingMemberId;
        long oldMemberId;
        long firstGuestId;

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            firstBoardId = insertGameBoard(statement);
            secondBoardId = insertGameBoard(statement);
            firstExerciseId = insertExercise(statement, firstBoardId, "2035-06-30");
            secondExerciseId = insertExercise(statement, secondBoardId, "2030-01-01");

            firstMemberId = insertMember(statement, "첫 회원", "FEMALE", "B", "2000-07-01", 93001L);
            existingMemberId = insertMember(statement, "기존 회원", "MALE", "A", "1995-01-01", 93002L);
            oldMemberId = insertMember(statement, "고령 회원", "MALE", "C", "1940-01-01", 93003L);

            insertMemberExercise(statement, firstExerciseId, firstMemberId, "2035-01-01 10:00:00.000000");
            insertMemberExercise(statement, firstExerciseId, firstMemberId, "2035-02-01 10:00:00.000000");
            insertMemberExercise(statement, secondExerciseId, existingMemberId, "2029-01-01 10:00:00.000000");
            insertMemberExercise(statement, secondExerciseId, oldMemberId, "2029-02-01 10:00:00.000000");

            firstGuestId = insertGuest(statement, firstExerciseId, "첫 게스트", "MALE", "D");
            insertGuest(statement, secondExerciseId, "둘째 게스트", "FEMALE", "C");

            insertExistingMemberRoster(statement, secondBoardId, existingMemberId);
            insertManualRoster(statement, firstBoardId);
        }

        migrateToTargetSchema();

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            assertThat(queryLong(statement, """
                    SELECT COUNT(*) FROM game_board_member
                    WHERE member_id IS NOT NULL OR guest_id IS NOT NULL
                    """))
                    .isEqualTo(5);
            assertThat(queryLong(statement, """
                    SELECT COUNT(*) FROM member_exercise
                    WHERE exercise_id = %d AND member_id = %d
                    """.formatted(firstExerciseId, firstMemberId)))
                    .isEqualTo(1);
            assertThat(queryLong(statement, "SELECT COUNT(*) FROM game_board_member"))
                    .isEqualTo(6);

            assertThat(queryLong(statement, """
                    SELECT COUNT(*) FROM game_board_member
                    WHERE game_board_id = %d AND member_id = %d
                    """.formatted(secondBoardId, existingMemberId)))
                    .isEqualTo(1);
            assertThat(queryString(statement, """
                    SELECT name FROM game_board_member
                    WHERE game_board_id = %d AND member_id = %d
                    """.formatted(secondBoardId, existingMemberId)))
                    .isEqualTo("기존 명단");

            assertThat(queryString(statement, """
                    SELECT age_group FROM game_board_member
                    WHERE game_board_id = %d AND member_id = %d
                    """.formatted(firstBoardId, firstMemberId)))
                    .isEqualTo("THIRTIES");
            assertThat(queryNullableString(statement, """
                    SELECT age_group FROM game_board_member
                    WHERE game_board_id = %d AND member_id = %d
                    """.formatted(secondBoardId, oldMemberId)))
                    .isNull();

            assertThat(queryString(statement, """
                    SELECT name FROM game_board_member
                    WHERE game_board_id = %d AND guest_id = %d
                    """.formatted(firstBoardId, firstGuestId)))
                    .isEqualTo("첫 게스트");
            assertThat(queryNullableString(statement, """
                    SELECT age_group FROM game_board_member
                    WHERE game_board_id = %d AND guest_id = %d
                    """.formatted(firstBoardId, firstGuestId)))
                    .isNull();

            assertThat(queryLong(statement, """
                    SELECT COUNT(*) FROM game_board_member
                    WHERE shuttlecock_submitted = 0
                      AND participating = 1
                      AND game_count = 0
                    """))
                    .isEqualTo(6);
            assertThat(queryLong(statement, """
                    SELECT COUNT(*)
                    FROM game_board_member
                    WHERE member_id = %d
                      AND created_at = '2035-01-01 10:00:00.000000'
                    """.formatted(firstMemberId)))
                    .isEqualTo(1);
            assertThat(queryLong(statement, """
                    SELECT COUNT(*)
                    FROM game_board_member
                    WHERE name = '수동 명단'
                      AND member_id IS NULL
                      AND guest_id IS NULL
                    """))
                    .isEqualTo(1);

            assertThatThrownBy(() -> insertMemberExercise(
                    statement, firstExerciseId, firstMemberId, "2035-03-01 10:00:00.000000"))
                    .isInstanceOf(SQLException.class);
        }
    }

    private void migrateToRosterSourceSchema() {
        flyway().target(ROSTER_SOURCE_SCHEMA_VERSION).load().migrate();
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

    private long insertGameBoard(Statement statement) throws SQLException {
        statement.executeUpdate("INSERT INTO game_board (created_at, updated_at) VALUES (NOW(6), NOW(6))");
        return generatedId(statement);
    }

    private long insertExercise(Statement statement, long gameBoardId, String date) throws SQLException {
        statement.executeUpdate("""
                INSERT INTO exercise (
                    created_at, updated_at, date, start_time, max_capacity,
                    party_guest_accept, outside_guest_accept, game_board_id
                ) VALUES (NOW(6), NOW(6), '%s', '10:00:00', 10, 1, 1, %d)
                """.formatted(date, gameBoardId));
        return generatedId(statement);
    }

    private long insertMember(
            Statement statement, String name, String gender, String level, String birth, long socialId)
            throws SQLException {
        statement.executeUpdate("""
                INSERT INTO member (member_name, gender, birth, level, social_id)
                VALUES ('%s', '%s', '%s', '%s', %d)
                """.formatted(name, gender, birth, level, socialId));
        return generatedId(statement);
    }

    private void insertMemberExercise(
            Statement statement, long exerciseId, long memberId, String createdAt) throws SQLException {
        statement.executeUpdate("""
                INSERT INTO member_exercise (
                    created_at, updated_at, member_id, exercise_id, exercise_member_ship_status
                ) VALUES ('%s', '%s', %d, %d, 'PARTY_MEMBER')
                """.formatted(createdAt, createdAt, memberId, exerciseId));
    }

    private long insertGuest(
            Statement statement, long exerciseId, String name, String gender, String level) throws SQLException {
        statement.executeUpdate("""
                INSERT INTO guest (created_at, updated_at, exercise_id, guest_name, gender, level, inviter_id)
                VALUES (NOW(6), NOW(6), %d, '%s', '%s', '%s', 1)
                """.formatted(exerciseId, name, gender, level));
        return generatedId(statement);
    }

    private void insertExistingMemberRoster(
            Statement statement, long gameBoardId, long memberId) throws SQLException {
        statement.executeUpdate("""
                INSERT INTO game_board_member (
                    game_board_id, member_id, name, gender, level,
                    shuttlecock_submitted, participating, game_count
                ) VALUES (%d, %d, '기존 명단', 'MALE', 'A', 0, 1, 0)
                """.formatted(gameBoardId, memberId));
    }

    private void insertManualRoster(Statement statement, long gameBoardId) throws SQLException {
        statement.executeUpdate("""
                INSERT INTO game_board_member (
                    game_board_id, name, gender, level,
                    shuttlecock_submitted, participating, game_count
                ) VALUES (%d, '수동 명단', 'FEMALE', 'B', 0, 1, 0)
                """.formatted(gameBoardId));
    }

    private long generatedId(Statement statement) throws SQLException {
        return queryLong(statement, "SELECT LAST_INSERT_ID()");
    }

    private long queryLong(Statement statement, String sql) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private String queryString(Statement statement, String sql) throws SQLException {
        String value = queryNullableString(statement, sql);
        if (value == null) {
            throw new AssertionError("Expected non-null query result");
        }
        return value;
    }

    private String queryNullableString(Statement statement, String sql) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }
}
