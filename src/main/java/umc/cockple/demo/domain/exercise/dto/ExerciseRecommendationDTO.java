package umc.cockple.demo.domain.exercise.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class ExerciseRecommendationDTO {

    public record Resposne(
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
    ) {
    }
}
