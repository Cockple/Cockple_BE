package umc.cockple.demo.domain.exercise.service.query.result;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Builder
public record ExerciseMapBuildingsResult(
        Integer year,
        Integer month,
        Double centerLatitude,
        Double centerLongitude,
        Double radiusKm,
        Map<LocalDate, List<BuildingInfo>> buildings
) {

    @Builder
    public record BuildingInfo(
            String buildingName,
            String streetAddr,
            Double latitude,
            Double longitude
    ) {
    }
}
