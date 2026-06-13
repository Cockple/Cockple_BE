package umc.cockple.demo.domain.exercise.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("ExerciseMapBoundingBox")
class ExerciseMapBoundingBoxTest {

    @Test
    @DisplayName("중심 좌표와 반경으로 bounding box 후보 영역을 계산한다")
    void 중심_좌표와_반경으로_bounding_box_후보_영역을_계산한다() {
        // when
        ExerciseMapBoundingBox boundingBox = ExerciseMapBoundingBox.from(37.5, 127.0, 3.9);

        // then
        assertThat(boundingBox.minLatitude()).isCloseTo(37.46487910306632, within(1.0e-12));
        assertThat(boundingBox.maxLatitude()).isCloseTo(37.53512089693368, within(1.0e-12));
        assertThat(boundingBox.minLongitude()).isCloseTo(126.9557310782598, within(1.0e-12));
        assertThat(boundingBox.maxLongitude()).isCloseTo(127.0442689217402, within(1.0e-12));
        assertThat(boundingBox.isWithinCoordinateRange()).isTrue();
    }

    @Test
    @DisplayName("bounding box가 SRID 4326 좌표 범위를 벗어나는지 확인한다")
    void bounding_box가_srid_4326_좌표_범위를_벗어나는지_확인한다() {
        // when
        ExerciseMapBoundingBox boundingBox = ExerciseMapBoundingBox.from(37.5, 127.0, 10_000.0);

        // then
        assertThat(boundingBox.isWithinCoordinateRange()).isFalse();
    }
}
