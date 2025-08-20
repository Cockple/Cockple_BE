package umc.cockple.demo.domain.exercise.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class MyExerciseListDTO {

    @Builder
    public record Response(
            Integer totalCount,
            Boolean hasNext,
            List<ExerciseItem> exercises
    ) {
    }

    @Builder
    public record ExerciseItem(
            Long exerciseId,
            Long partyId,
            String partyName,
            Boolean isBookmarked,
            LocalDate date,
            String dayOfWeek,
            String buildingName,
            LocalTime startTime,
            LocalTime endTime,
            List<String> femaleLevel,
            List<String> maleLevel,
            Integer currentParticipants,
            Integer maxCapacity,
            Boolean isCompleted,
            Boolean partyGuestInviteAccept
    ) {
    }
}
