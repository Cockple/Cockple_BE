package umc.cockple.demo.domain.exercise.service.support.assembler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseBuildingDetailResult;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseMapBuildingsResult;
import umc.cockple.demo.domain.file.service.ImageUrlResolver;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyImg;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ExerciseMapResultAssembler {

    private final ImageUrlResolver imageUrlResolver;

    public ExerciseBuildingDetailResult toEmptyBuildingDetailResult(String buildingName, LocalDate date) {
        return ExerciseBuildingDetailResult.builder()
                .date(date)
                .buildingName(buildingName)
                .exercises(List.of())
                .build();
    }

    public ExerciseBuildingDetailResult toBuildingDetailResult(
            List<Exercise> exercises, String buildingName, Map<Long, Boolean> bookmarkStatus, LocalDate date) {

        List<ExerciseBuildingDetailResult.ExerciseItem> finalExercises = exercises.stream()
                .map(exercise -> toBuildingDetailItem(exercise, bookmarkStatus))
                .toList();

        return ExerciseBuildingDetailResult.builder()
                .date(date)
                .buildingName(buildingName)
                .exercises(finalExercises)
                .build();
    }

    public ExerciseMapBuildingsResult toMapCalendarSummaryResult(
            Integer year,
            Integer month,
            Double latitude,
            Double longitude,
            Double radiusKm,
            Map<LocalDate, List<ExerciseMapBuildingsResult.BuildingInfo>> buildings) {

        return ExerciseMapBuildingsResult.builder()
                .year(year)
                .month(month)
                .centerLatitude(latitude)
                .centerLongitude(longitude)
                .radiusKm(radiusKm)
                .buildings(buildings)
                .build();
    }

    public ExerciseMapBuildingsResult.BuildingInfo toBuildingSummary(
            String name, String address, Double latitude, Double longitude) {
        return ExerciseMapBuildingsResult.BuildingInfo.builder()
                .buildingName(name)
                .streetAddr(address)
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }

    private ExerciseBuildingDetailResult.ExerciseItem toBuildingDetailItem(
            Exercise exercise, Map<Long, Boolean> bookmarkStatus) {

        Party party = exercise.getParty();

        return ExerciseBuildingDetailResult.ExerciseItem.builder()
                .exerciseId(exercise.getId())
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .profileImageUrl(imageUrlResolver.resolve(party.getPartyImg(), PartyImg::getImgKey))
                .bookmarked(bookmarkStatus.getOrDefault(exercise.getId(), false))
                .startTime(exercise.getStartTime())
                .endTime(exercise.getEndTime())
                .build();
    }
}
