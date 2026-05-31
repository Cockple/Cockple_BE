package umc.cockple.demo.domain.exercise.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import umc.cockple.demo.support.IntegrationTestBase;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExerciseRepository Spatial 조회")
class ExerciseRepositorySpatialIntegrationTest extends IntegrationTestBase {

    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        deleteSpatialExplainData();

        Long nearAddrId = insertExerciseAddr("공간인덱스테스트1", 37.5, 127.0);
        Long fractionalRadiusAddrId = insertExerciseAddr("공간인덱스테스트2", 37.535, 127.0);
        Long farAddrId = insertExerciseAddr("공간인덱스테스트3", 35.17, 129.13);

        insertExercise(nearAddrId, LocalDate.of(2026, 4, 3), "09:00:00");
        insertExercise(fractionalRadiusAddrId, LocalDate.of(2026, 4, 7), "15:00:00");
        insertExercise(farAddrId, LocalDate.of(2026, 4, 6), "14:00:00");
    }

    @AfterEach
    void tearDown() {
        deleteSpatialExplainData();
    }

    @Test
    @DisplayName("월간 지도 spatial 후보 조회는 exercise_addr location 공간 인덱스를 사용할 수 있는 형태를 유지한다")
    void 월간_지도_spatial_후보_조회는_exercise_addr_location_공간_인덱스를_사용할_수_있는_형태를_유지한다() {
        String centerPointWkt = "POINT(127.0 37.5)";
        String boundingBoxWkt = "POLYGON((126.95 37.45,127.05 37.45,127.05 37.55,126.95 37.55,126.95 37.45))";

        List<Map<String, Object>> planRows = jdbcTemplate.queryForList("""
                EXPLAIN
                SELECT e.id
                FROM exercise_addr addr
                JOIN exercise e ON e.addr_id = addr.id
                WHERE e.date BETWEEN ? AND ?
                AND MBRWithin(
                    addr.location,
                    ST_GeomFromText(?, 4326, 'axis-order=long-lat')
                )
                AND ST_Distance_Sphere(
                    addr.location,
                    ST_GeomFromText(?, 4326, 'axis-order=long-lat')
                ) <= (? * 1000.0)
                ORDER BY e.date ASC, e.start_time ASC
                """,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                boundingBoxWkt,
                centerPointWkt,
                3.9);

        Map<String, Object> addrPlan = planRows.stream()
                .filter(row -> "addr".equals(Objects.toString(row.get("table"), "")))
                .findFirst()
                .orElseThrow();

        String possibleKeys = Objects.toString(addrPlan.get("possible_keys"), "");
        String selectedKey = Objects.toString(addrPlan.get("key"), "");
        String accessType = Objects.toString(addrPlan.get("type"), "");

        assertThat(possibleKeys).contains("idx_exercise_addr_location");
        assertThat(selectedKey).isEqualTo("idx_exercise_addr_location");
        assertThat(accessType).isEqualTo("range");
    }

    private Long insertExerciseAddr(String buildingName, double latitude, double longitude) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO exercise_addr (
                        addr1, addr2, street_addr, building_name, latitude, longitude
                    ) VALUES (?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, "서울특별시");
            ps.setString(2, "강남구");
            ps.setString(3, "서울특별시 강남구 테헤란로");
            ps.setString(4, buildingName);
            ps.setDouble(5, latitude);
            ps.setDouble(6, longitude);
            return ps;
        }, keyHolder);

        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    private void insertExercise(Long addrId, LocalDate date, String startTime) {
        jdbcTemplate.update("""
                INSERT INTO exercise (
                    addr_id, date, start_time, max_capacity,
                    party_guest_accept, outside_guest_accept, notice
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, addrId, date, startTime, 12, true, true, "spatial-explain-test");
    }

    private void deleteSpatialExplainData() {
        jdbcTemplate.update("DELETE FROM exercise WHERE notice = 'spatial-explain-test'");
        jdbcTemplate.update("DELETE FROM exercise_addr WHERE building_name LIKE '공간인덱스테스트%'");
    }
}
