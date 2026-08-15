package umc.cockple.demo.domain.exercise.service.query.result;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Builder
public record ExerciseRecommendationResult(
        int totalExercises,
        List<ExerciseItem> exercises
) {

    @Builder
    public record ExerciseItem(
            Long exerciseId,
            Long partyId,
            String partyName,
            LocalDate date,
            String dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            String buildingName,
            String profileImageUrl,
            boolean bookmarked
    ) {
    }
}
