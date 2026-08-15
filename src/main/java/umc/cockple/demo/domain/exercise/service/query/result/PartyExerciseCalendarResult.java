package umc.cockple.demo.domain.exercise.service.query.result;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Builder
public record PartyExerciseCalendarResult(
        LocalDate startDate,
        LocalDate endDate,
        boolean member,
        String partyName,
        List<WeeklyExercises> weeks
) {

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
            boolean bookmarked,
            LocalTime startTime,
            LocalTime endTime,
            String buildingName,
            List<String> femaleLevel,
            List<String> maleLevel,
            int currentParticipants,
            int maxCapacity,
            boolean participating
    ) {
    }
}
