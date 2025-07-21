package umc.cockple.demo.domain.exercise.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class PartyExerciseCalenderDTO {

    @Builder
    public record Response(
            LocalDate startDate,
            LocalDate endDate,
            Boolean isMember,
            String partyName,
            List<WeeklyExercises> weeks
    ) {
    }

    @Builder
    public record WeeklyExercises(
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            List<ExerciseCalendarItem> exercises
    ) {
    }

    @Builder
    public record ExerciseCalendarItem(
            Long exerciseId,
            LocalDate date,
            String dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            String buildingName,
            List<String> femaleLevel,
            List<String> maleLevel,
            Integer currentParticipants,
            Integer maxCapacity
    ) {
    }
}
