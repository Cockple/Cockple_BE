package umc.cockple.demo.domain.exercise.dto.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ExerciseMapBuildingsDTO")
class ExerciseMapBuildingsDTOTest {

    @Nested
    @DisplayName("Query")
    class QueryTest {

        @Test
        @DisplayName("좌표가 없으면 대표주소 fallback 전 상태로 생성된다")
        void 좌표가_없으면_대표주소_fallback_전_상태로_생성된다() {
            // when
            ExerciseMapBuildingsDTO.Query query = ExerciseMapBuildingsDTO.Query.of(
                    LocalDate.of(2026, 4, 1), null, null, 3.9);

            // then
            assertThat(query.latitude()).isNull();
            assertThat(query.longitude()).isNull();
            assertThat(query.radiusKm()).isEqualTo(3.9);
        }

        @Test
        @DisplayName("명시 좌표와 반경이 유효하면 생성된다")
        void 명시_좌표와_반경이_유효하면_생성된다() {
            // when
            ExerciseMapBuildingsDTO.Query query = ExerciseMapBuildingsDTO.Query.of(
                    LocalDate.of(2026, 4, 1), 37.5, 127.0, 3.9);

            // then
            assertThat(query.latitude()).isEqualTo(37.5);
            assertThat(query.longitude()).isEqualTo(127.0);
            assertThat(query.radiusKm()).isEqualTo(3.9);
        }

        @Test
        @DisplayName("대표주소 fallback 좌표가 유효하면 좌표가 채워진 Query를 반환한다")
        void 대표주소_fallback_좌표가_유효하면_좌표가_채워진_query를_반환한다() {
            // given
            ExerciseMapBuildingsDTO.Query query = ExerciseMapBuildingsDTO.Query.of(
                    LocalDate.of(2026, 4, 1), null, null, 3.9);

            // when
            ExerciseMapBuildingsDTO.Query fallbackQuery = query.withFallbackLocation(37.5, 127.0);

            // then
            assertThat(fallbackQuery.latitude()).isEqualTo(37.5);
            assertThat(fallbackQuery.longitude()).isEqualTo(127.0);
            assertThat(fallbackQuery.radiusKm()).isEqualTo(3.9);
        }

        @Test
        @DisplayName("위도와 경도 중 하나만 있으면 예외를 던진다")
        void 위도와_경도_중_하나만_있으면_예외를_던진다() {
            assertThatThrownBy(() -> ExerciseMapBuildingsDTO.Query.of(
                    LocalDate.of(2026, 4, 1), 37.5, null, 3.9))
                    .isInstanceOf(ExerciseException.class)
                    .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.INCOMPLETE_LOCATION_INFO);
        }

        @Test
        @DisplayName("좌표 범위를 벗어나면 예외를 던진다")
        void 좌표_범위를_벗어나면_예외를_던진다() {
            assertThatThrownBy(() -> ExerciseMapBuildingsDTO.Query.of(
                    LocalDate.of(2026, 4, 1), 91.0, 127.0, 3.9))
                    .isInstanceOf(ExerciseException.class)
                    .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.INVALID_LOCATION_INFO);
        }

        @Test
        @DisplayName("반경이 양수가 아니면 예외를 던진다")
        void 반경이_양수가_아니면_예외를_던진다() {
            assertThatThrownBy(() -> ExerciseMapBuildingsDTO.Query.of(
                    LocalDate.of(2026, 4, 1), 37.5, 127.0, 0.0))
                    .isInstanceOf(ExerciseException.class)
                    .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.INVALID_LOCATION_INFO);
        }

        @Test
        @DisplayName("반경 bounding box가 SRID 4326 좌표 범위를 벗어나면 예외를 던진다")
        void 반경_bounding_box가_srid_4326_좌표_범위를_벗어나면_예외를_던진다() {
            assertThatThrownBy(() -> ExerciseMapBuildingsDTO.Query.of(
                    LocalDate.of(2026, 4, 1), 37.5, 127.0, 10_000.0))
                    .isInstanceOf(ExerciseException.class)
                    .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.INVALID_LOCATION_INFO);
        }

        @Test
        @DisplayName("대표주소 fallback 좌표가 유효하지 않으면 예외를 던진다")
        void 대표주소_fallback_좌표가_유효하지_않으면_예외를_던진다() {
            // given
            ExerciseMapBuildingsDTO.Query query = ExerciseMapBuildingsDTO.Query.of(
                    LocalDate.of(2026, 4, 1), null, null, 3.9);

            // when & then
            assertThatThrownBy(() -> query.withFallbackLocation(91.0, 127.0))
                    .isInstanceOf(ExerciseException.class)
                    .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.INVALID_LOCATION_INFO);
        }
    }
}
