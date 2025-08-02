package umc.cockple.demo.domain.exercise.repository;

import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.dto.ExerciseRecommendationCalendarDTO;

import java.time.LocalDate;
import java.util.List;

public interface ExerciseRepositoryCustom {

    List<Exercise> findFilteredRecommendedExercisesForCalendar(
            Long memberId, Integer memberBirthYear, ExerciseRecommendationCalendarDTO.FilterSortType filterSortType,
            LocalDate startDate, LocalDate endDate);
}
