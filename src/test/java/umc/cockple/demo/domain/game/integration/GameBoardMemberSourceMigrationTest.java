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
@DisplayName("게임판 명단 원본 연결 마이그레이션")
class GameBoardMemberSourceMigrationTest {

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.36");

    @Test
    @DisplayName("원본 연결의 무결성과 회원 스냅샷 보존·게스트 명단 정리를 보장한다")
    void addGameBoardMemberSources() throws Exception {
        flyway().migrate();

        try (Connection connection = mysql.createConnection("");
             Statement statement = connection.createStatement()) {
            long firstBoardId = insertGameBoard(statement);
            long secondBoardId = insertGameBoard(statement);
            long memberId = insertMember(statement);
            long exerciseId = insertExercise(statement, firstBoardId);
            long guestId = insertGuest(statement, exerciseId, memberId);

            insertMemberSource(statement, firstBoardId, memberId, "회원 스냅샷");
            insertMemberSource(statement, secondBoardId, memberId, "다른 게임판 회원 스냅샷");
            insertGuestSource(statement, firstBoardId, guestId, "게스트 스냅샷");

            assertThatThrownBy(() ->
                    insertMemberSource(statement, firstBoardId, memberId, "중복 회원"))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() ->
                    insertGuestSource(statement, firstBoardId, guestId, "중복 게스트"))
                    .isInstanceOf(SQLException.class);
            statement.executeUpdate("DELETE FROM member WHERE id = " + memberId);

            assertThat(queryLong(statement, """
                    SELECT COUNT(*) FROM game_board_member
                    WHERE name = '회원 스냅샷' AND member_id IS NULL
                    """))
                    .isEqualTo(1);
            assertThat(queryString(statement, """
                    SELECT name FROM game_board_member
                    WHERE game_board_id = %d AND name = '회원 스냅샷' AND member_id IS NULL
                    """.formatted(firstBoardId)))
                    .isEqualTo("회원 스냅샷");

            statement.executeUpdate("DELETE FROM guest WHERE id = " + guestId);

            assertThat(queryLong(statement, """
                    SELECT COUNT(*) FROM game_board_member
                    WHERE name = '게스트 스냅샷'
                    """))
                    .isZero();
        }
    }

    private Flyway flyway() {
        return Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .locations("classpath:db/migration")
                .load();
    }

    private long insertGameBoard(Statement statement) throws SQLException {
        statement.executeUpdate("INSERT INTO game_board (created_at, updated_at) VALUES (NOW(6), NOW(6))");
        return generatedId(statement);
    }

    private long insertMember(Statement statement) throws SQLException {
        statement.executeUpdate("""
                INSERT INTO member (member_name, gender, birth, level, social_id)
                VALUES ('회원', 'MALE', '2000-01-01', 'A', 9100001)
                """);
        return generatedId(statement);
    }

    private long insertExercise(Statement statement, long gameBoardId) throws SQLException {
        statement.executeUpdate("""
                INSERT INTO exercise (
                    date, start_time, max_capacity,
                    party_guest_accept, outside_guest_accept, game_board_id
                ) VALUES ('2099-12-31', '10:00:00', 10, 1, 1, %d)
                """.formatted(gameBoardId));
        return generatedId(statement);
    }

    private long insertGuest(Statement statement, long exerciseId, long inviterId) throws SQLException {
        statement.executeUpdate("""
                INSERT INTO guest (exercise_id, guest_name, gender, level, inviter_id)
                VALUES (%d, '게스트', 'FEMALE', 'B', %d)
                """.formatted(exerciseId, inviterId));
        return generatedId(statement);
    }

    private void insertMemberSource(
            Statement statement, long gameBoardId, long memberId, String name) throws SQLException {
        statement.executeUpdate("""
                INSERT INTO game_board_member (
                    game_board_id, member_id, name, gender, level,
                    age_group, shuttlecock_submitted, participating, game_count
                ) VALUES (%d, %d, '%s', 'MALE', 'A', 'THIRTIES', 0, 1, 0)
                """.formatted(gameBoardId, memberId, name));
    }

    private void insertGuestSource(
            Statement statement, long gameBoardId, long guestId, String name) throws SQLException {
        statement.executeUpdate("""
                INSERT INTO game_board_member (
                    game_board_id, guest_id, name, gender, level,
                    age_group, shuttlecock_submitted, participating, game_count
                ) VALUES (%d, %d, '%s', 'FEMALE', 'B', NULL, 0, 1, 0)
                """.formatted(gameBoardId, guestId, name));
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
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }
}
