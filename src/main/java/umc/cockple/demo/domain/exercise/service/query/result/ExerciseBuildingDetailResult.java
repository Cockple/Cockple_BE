package umc.cockple.demo.domain.exercise.service.query.result;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Builder
public record ExerciseBuildingDetailResult(
        LocalDate date,
        String buildingName,
        List<ExerciseItem> exercises
) {

    @Builder
    public record ExerciseItem(
            Long exerciseId,
            Long partyId,
            String partyName,
            String profileImageUrl,
            boolean bookmarked,
            LocalTime startTime,
            LocalTime endTime
    ) {
    }
}
