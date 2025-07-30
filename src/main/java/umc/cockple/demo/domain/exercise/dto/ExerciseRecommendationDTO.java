package umc.cockple.demo.domain.exercise.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class ExerciseRecommendationDTO {

    @Builder
    public record Response(
            Integer totalExercises,
            List<ExerciseItem> exercises
    ) {
    }

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
            String imageUrl,
            Boolean isBookmarked
    ){

    }
}
