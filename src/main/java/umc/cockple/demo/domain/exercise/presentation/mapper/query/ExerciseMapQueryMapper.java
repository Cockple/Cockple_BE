package umc.cockple.demo.domain.exercise.presentation.mapper.query;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.presentation.dto.map.ExerciseBuildingDetailDTO;
import umc.cockple.demo.domain.exercise.presentation.dto.map.ExerciseMapBuildingsDTO;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseBuildingDetailResult;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseMapBuildingsResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
public class ExerciseMapQueryMapper {

    public ExerciseBuildingDetailDTO.Response toBuildingDetailResponse(
            ExerciseBuildingDetailResult result) {
        return ExerciseBuildingDetailDTO.Response.builder()
                .date(result.date())
                .dayOfWeek(result.date().getDayOfWeek().name())
                .buildingName(result.buildingName())
                .exercises(result.exercises().stream().map(this::toBuildingDetailItem).toList())
                .build();
    }

    public ExerciseMapBuildingsDTO.Response toMapCalendarSummaryResponse(
            ExerciseMapBuildingsResult result) {
        return ExerciseMapBuildingsDTO.Response.builder()
                .year(result.year())
                .month(result.month())
                .centerLatitude(result.centerLatitude())
                .centerLongitude(result.centerLongitude())
                .radiusKm(result.radiusKm())
                .buildings(toBuildingInfoMap(result.buildings()))
                .build();
    }

    private ExerciseBuildingDetailDTO.ExerciseItem toBuildingDetailItem(
            ExerciseBuildingDetailResult.ExerciseItem result) {
        return ExerciseBuildingDetailDTO.ExerciseItem.builder()
                .exerciseId(result.exerciseId())
                .partyId(result.partyId())
                .partyName(result.partyName())
                .profileImageUrl(result.profileImageUrl())
                .isBookmarked(result.bookmarked())
                .startTime(result.startTime())
                .endTime(result.endTime())
                .build();
    }

    private Map<LocalDate, List<ExerciseMapBuildingsDTO.BuildingInfo>> toBuildingInfoMap(
            Map<LocalDate, List<ExerciseMapBuildingsResult.BuildingInfo>> buildings) {
        return buildings.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().map(this::toBuildingInfo).toList(),
                        (existing, replacement) -> existing,
                        TreeMap::new
                ));
    }

    private ExerciseMapBuildingsDTO.BuildingInfo toBuildingInfo(
            ExerciseMapBuildingsResult.BuildingInfo result) {
        return ExerciseMapBuildingsDTO.BuildingInfo.builder()
                .buildingName(result.buildingName())
                .streetAddr(result.streetAddr())
                .latitude(result.latitude())
                .longitude(result.longitude())
                .build();
    }
}
