package umc.cockple.demo.domain.exercise.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ExerciseMapCalendarSummaryDTO {

    @Builder
    public record Response(
            Integer year,
            Integer month,
            Double centerLatitude,
            Double centerLongitude,
            Double radiusKm,
            Map<LocalDate, List<BuildingSummary>> buildings) {
    }

    @Builder
    public record BuildingSummary(
            String buildingName,
            String streetAddr,
            Double latitude,
            Double longitude
    ) {
    }
}
