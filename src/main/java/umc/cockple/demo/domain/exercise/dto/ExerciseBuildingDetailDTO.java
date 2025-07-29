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
            List<ExerciseDetail> exercises
    ) {
    }

    @Builder
    public record ExerciseDetail(
            Long exerciseId,
            String partyName,
            String partyImgUrl,
            Boolean isBookmarked,
            LocalTime startTime,
            LocalTime endTime
    ) {
    }
}
