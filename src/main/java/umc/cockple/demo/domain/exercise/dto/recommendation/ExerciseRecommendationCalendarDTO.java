package umc.cockple.demo.domain.exercise.dto.recommendation;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class ExerciseRecommendationCalendarDTO {

    @Builder
    public record Response(
            LocalDate startDate,
            LocalDate endDate,
            List<WeeklyExercises> weeks
    ) {
    }

    @Builder
    public record WeeklyExercises(
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            List<DailyExercises> days
    ) {
    }

    @Builder
    public record DailyExercises(
            LocalDate date,
            String dayOfWeek,
            List<ExerciseCalendarItem> exercises
    ) {
    }

    @Builder
    public record ExerciseCalendarItem(
            Long exerciseId,
            Long partyId,
            String partyName,
            String buildingName,
            LocalTime startTime,
            LocalTime endTime,
            String profileImageUrl,
            Boolean isBookmarked,
            Double distance
    ) {
    }
}
