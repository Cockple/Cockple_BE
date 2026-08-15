package umc.cockple.demo.domain.exercise.service.query.result;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Builder
public record MyExerciseListResult(
        int totalCount,
        boolean hasNext,
        List<ExerciseItem> exercises
) {

    @Builder
    public record ExerciseItem(
            Long exerciseId,
            Long partyId,
            String partyName,
            boolean bookmarked,
            LocalDate date,
            String dayOfWeek,
            String buildingName,
            LocalTime startTime,
            LocalTime endTime,
            List<String> femaleLevel,
            List<String> maleLevel,
            int currentParticipants,
            int maxCapacity,
            boolean completed,
            boolean partyGuestInviteAccept
    ) {
    }
}
