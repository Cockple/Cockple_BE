package umc.cockple.demo.domain.exercise.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.repository.support.ExerciseMapSpatialSearchCondition;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

@DisplayName("ExerciseRepository")
class ExerciseRepositoryTest {

    @Test
    @DisplayName("월간 지도 반경 조회는 ID 후보군을 먼저 조회한 뒤 실제 엔티티를 조회한다")
    void 월간_지도_반경_조회는_id_후보군을_먼저_조회한_뒤_실제_엔티티를_조회한다() {
        // given
        ExerciseRepository exerciseRepository = mock(
                ExerciseRepository.class,
                withSettings().defaultAnswer(CALLS_REAL_METHODS));
        LocalDate startDate = LocalDate.of(2026, 4, 1);
        LocalDate endDate = LocalDate.of(2026, 4, 30);
        double latitude = 37.5;
        double longitude = 127.0;
        double radiusKm = 3.9;
        ExerciseMapSpatialSearchCondition searchCondition =
                ExerciseMapSpatialSearchCondition.from(latitude, longitude, radiusKm);
        List<Long> exerciseIds = List.of(10L, 20L);
        List<Exercise> expectedExercises = List.of(mock(Exercise.class), mock(Exercise.class));

        doReturn(exerciseIds).when(exerciseRepository).findExerciseIdsByMonthAndRadius(
                startDate,
                endDate,
                searchCondition.centerPointWkt(),
                searchCondition.boundingBoxWkt(),
                searchCondition.radiusKm());
        doReturn(expectedExercises).when(exerciseRepository).findExercisesByIdsForMonthlyMap(exerciseIds);

        // when
        List<Exercise> exercises = exerciseRepository.findExercisesByMonthAndRadius(
                startDate, endDate, latitude, longitude, radiusKm);

        // then
        assertThat(exercises).isSameAs(expectedExercises);
        var inOrder = inOrder(exerciseRepository);
        inOrder.verify(exerciseRepository).findExerciseIdsByMonthAndRadius(
                startDate,
                endDate,
                searchCondition.centerPointWkt(),
                searchCondition.boundingBoxWkt(),
                searchCondition.radiusKm());
        inOrder.verify(exerciseRepository).findExercisesByIdsForMonthlyMap(exerciseIds);
    }

    @Test
    @DisplayName("월간 지도 ID 후보군이 없으면 실제 엔티티 조회를 생략한다")
    void 월간_지도_id_후보군이_없으면_실제_엔티티_조회를_생략한다() {
        // given
        ExerciseRepository exerciseRepository = mock(
                ExerciseRepository.class,
                withSettings().defaultAnswer(CALLS_REAL_METHODS));
        LocalDate startDate = LocalDate.of(2026, 4, 1);
        LocalDate endDate = LocalDate.of(2026, 4, 30);
        double latitude = 37.5;
        double longitude = 127.0;
        double radiusKm = 3.9;
        ExerciseMapSpatialSearchCondition searchCondition =
                ExerciseMapSpatialSearchCondition.from(latitude, longitude, radiusKm);

        doReturn(List.of()).when(exerciseRepository).findExerciseIdsByMonthAndRadius(
                startDate,
                endDate,
                searchCondition.centerPointWkt(),
                searchCondition.boundingBoxWkt(),
                searchCondition.radiusKm());

        // when
        List<Exercise> exercises = exerciseRepository.findExercisesByMonthAndRadius(
                startDate, endDate, latitude, longitude, radiusKm);

        // then
        assertThat(exercises).isEmpty();
        verify(exerciseRepository, never()).findExercisesByIdsForMonthlyMap(anyList());
    }
}
