package umc.cockple.demo.domain.exercise.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class MyExerciseCalendarDTO {

    @Builder
    public record Response(
            LocalDate startDate,
            LocalDate endDate,
            List<MyExerciseCalendarDTO.WeeklyExercises> weeks
    ) {
    }

    @Builder
    public record WeeklyExercises(
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            List<MyExerciseCalendarDTO.DailyExercises> days
    ) {
    }

    @Builder
    public record DailyExercises(
            LocalDate date,
            String dayOfWeek,
            List<MyExerciseCalendarDTO.ExerciseCalendarItem> exercises
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
            String profileImageUrl
    ) {
    }
}
