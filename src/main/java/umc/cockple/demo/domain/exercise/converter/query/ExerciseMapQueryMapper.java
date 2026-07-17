package umc.cockple.demo.domain.exercise.converter.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.dto.ExerciseBuildingDetailDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseMapBuildingsDTO;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyImg;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ExerciseMapQueryMapper {

    private final FileService fileService;

    public ExerciseBuildingDetailDTO.Response toEmptyBuildingDetailResponse(String buildingName, LocalDate date) {
        return ExerciseBuildingDetailDTO.Response.builder()
                .date(date)
                .dayOfWeek(date.getDayOfWeek().name())
                .buildingName(buildingName)
                .exercises(List.of())
                .build();
    }

    public ExerciseBuildingDetailDTO.Response toBuildingDetailResponse(
            List<Exercise> exercises, String buildingName, Map<Long, Boolean> bookmarkStatus, LocalDate date) {

        List<ExerciseBuildingDetailDTO.ExerciseItem> finalExercises = exercises.stream()
                .map(exercise -> toBuildingDetailItem(exercise, bookmarkStatus))
                .toList();

        return ExerciseBuildingDetailDTO.Response.builder()
                .date(date)
                .dayOfWeek(date.getDayOfWeek().name())
                .buildingName(buildingName)
                .exercises(finalExercises)
                .build();
    }

    public ExerciseMapBuildingsDTO.Response toMapCalendarSummaryResponse(
            Integer year,
            Integer month,
            Double latitude,
            Double longitude,
            Double radiusKm,
            Map<LocalDate, List<ExerciseMapBuildingsDTO.BuildingInfo>> buildings) {

        return ExerciseMapBuildingsDTO.Response.builder()
                .year(year)
                .month(month)
                .centerLatitude(latitude)
                .centerLongitude(longitude)
                .radiusKm(radiusKm)
                .buildings(buildings)
                .build();
    }

    public ExerciseMapBuildingsDTO.BuildingInfo toBuildingSummary(
            String name, String address, Double latitude, Double longitude) {
        return ExerciseMapBuildingsDTO.BuildingInfo.builder()
                .buildingName(name)
                .streetAddr(address)
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }

    private ExerciseBuildingDetailDTO.ExerciseItem toBuildingDetailItem(
            Exercise exercise, Map<Long, Boolean> bookmarkStatus) {

        Party party = exercise.getParty();

        return ExerciseBuildingDetailDTO.ExerciseItem.builder()
                .exerciseId(exercise.getId())
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .profileImageUrl(getImageUrl(party.getPartyImg()))
                .isBookmarked(bookmarkStatus.getOrDefault(exercise.getId(), false))
                .startTime(exercise.getStartTime())
                .endTime(exercise.getEndTime())
                .build();
    }

    private String getImageUrl(PartyImg partyImg) {
        if (partyImg != null && partyImg.getImgKey() != null && !partyImg.getImgKey().isBlank()) {
            return fileService.getUrlFromKey(partyImg.getImgKey());
        }
        return null;
    }
}
