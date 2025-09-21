package umc.cockple.demo.domain.exercise.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class ExerciseBuildingDetailDTO {

    @Builder
    public record Response(
            LocalDate date,
            String dayOfWeek,
            String buildingName,
            List<ExerciseItem> exercises
    ) {
    }

    @Builder
    public record ExerciseItem(
            Long exerciseId,
            Long partyId,
            String partyName,
            String profileImageUrl,
            Boolean isBookmarked,
            LocalTime startTime,
            LocalTime endTime
    ) {
    }
}
