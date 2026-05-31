package umc.cockple.demo.domain.exercise.repository.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExerciseMapSpatialSearchCondition")
class ExerciseMapSpatialSearchConditionTest {

    @Test
    @DisplayName("MySQL long-lat 축 순서에 맞춰 중심점과 bounding box WKT를 생성한다")
    void mysql_long_lat_축_순서에_맞춰_중심점과_bounding_box_wkt를_생성한다() {
        // when
        ExerciseMapSpatialSearchCondition condition = ExerciseMapSpatialSearchCondition.from(37.5, 127.0, 3.9);

        // then
        assertThat(condition.centerPointWkt()).isEqualTo("POINT(127.0 37.5)");
        assertThat(condition.boundingBoxWkt()).isEqualTo(
                "POLYGON((126.9557310782598 37.46487910306632,"
                        + "127.0442689217402 37.46487910306632,"
                        + "127.0442689217402 37.53512089693368,"
                        + "126.9557310782598 37.53512089693368,"
                        + "126.9557310782598 37.46487910306632))");
        assertThat(condition.radiusKm()).isEqualTo(3.9);
    }

}
