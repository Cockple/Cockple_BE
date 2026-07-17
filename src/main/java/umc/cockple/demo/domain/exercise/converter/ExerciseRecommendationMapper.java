package umc.cockple.demo.domain.exercise.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.dto.ExerciseRecommendationCalendarDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseRecommendationDTO;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.MemberAddr;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyImg;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ExerciseRecommendationMapper {

    private final FileService fileService;

    public ExerciseRecommendationDTO.Response toExerciseRecommendationResponse(
            List<Exercise> finalExercises, Map<Long, Boolean> bookmarkStatus) {

        List<ExerciseRecommendationDTO.ExerciseItem> exercises = finalExercises.stream()
                .map(exercise -> toExerciseRecommendationItem(exercise, bookmarkStatus))
                .toList();

        return ExerciseRecommendationDTO.Response.builder()
                .totalExercises(finalExercises.size())
                .exercises(exercises)
                .build();
    }

    public ExerciseRecommendationCalendarDTO.Response toRecommendationCalendarResponse(
            List<Exercise> exercises,
            Map<Long, Boolean> bookmarkStatus,
            Map<Long, Integer> participantCountMap,
            MemberAddr mainAddr,
            LocalDate start,
            LocalDate end,
            Boolean isCockpleRecommend,
            ExerciseRecommendationCalendarDTO.FilterSortType filterSortType) {

        List<ExerciseRecommendationCalendarDTO.WeeklyExercises> weeks
                = groupRecommendedExerciseByWeek(exercises, bookmarkStatus, participantCountMap, mainAddr
                , start, end, isCockpleRecommend, filterSortType);

        return ExerciseRecommendationCalendarDTO.Response.builder()
                .startDate(start)
                .endDate(end)
                .weeks(weeks)
                .build();
    }

    private LocalDate getWeekStart(LocalDate date) {
        return date.minusDays(date.getDayOfWeek().getValue() - 1);
    }

    private List<Exercise> filterExercisesByWeek(List<Exercise> exercises, LocalDate weekStart, LocalDate weekEnd) {
        return exercises.stream()
                .filter(exercise -> {
                    LocalDate exerciseDate = exercise.getDate();
                    return !exerciseDate.isBefore(weekStart) && !exerciseDate.isAfter(weekEnd);
                })
                .toList();
    }

    private String getImageUrl(PartyImg partyImg) {
        if (partyImg != null && partyImg.getImgKey() != null && !partyImg.getImgKey().isBlank()) {
            return fileService.getUrlFromKey(partyImg.getImgKey());
        }
        return null;
    }

    private List<ExerciseRecommendationCalendarDTO.WeeklyExercises> groupRecommendedExerciseByWeek(
            List<Exercise> exercises,
            Map<Long, Boolean> bookmarkStatus,
            Map<Long, Integer> participantCountMap,
            MemberAddr mainAddr,
            LocalDate start,
            LocalDate end,
            Boolean isCockpleRecommend,
            ExerciseRecommendationCalendarDTO.FilterSortType filterSortType) {

        List<ExerciseRecommendationCalendarDTO.WeeklyExercises> weeks = new ArrayList<>();

        for (LocalDate weekStart = getWeekStart(start); !weekStart.isAfter(end); weekStart = weekStart.plusWeeks(1)) {
            LocalDate weekEnd = weekStart.plusDays(6);

            List<Exercise> weekExercises = filterExercisesByWeek(exercises, weekStart, weekEnd);

            List<ExerciseRecommendationCalendarDTO.DailyExercises> dailyExercisesList =
                    groupRecommendedExercisesByDate(weekExercises, weekStart, weekEnd, bookmarkStatus, participantCountMap, mainAddr, isCockpleRecommend, filterSortType);

            weeks.add(createRecommendedWeeklyExercises(weekStart, weekEnd, dailyExercisesList));
        }

        return weeks;
    }

    private List<ExerciseRecommendationCalendarDTO.DailyExercises> groupRecommendedExercisesByDate(
            List<Exercise> weekExercises,
            LocalDate weekStart,
            LocalDate weekEnd,
            Map<Long, Boolean> bookmarkStatus,
            Map<Long, Integer> participantCountMap,
            MemberAddr mainAddr,
            Boolean isCockpleRecommend,
            ExerciseRecommendationCalendarDTO.FilterSortType filterSortType) {

        Map<LocalDate, List<Exercise>> exercisesByDate = weekExercises.stream()
                .collect(Collectors.groupingBy(Exercise::getDate));

        List<ExerciseRecommendationCalendarDTO.DailyExercises> dailyExercisesList = new ArrayList<>();

        for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
            List<Exercise> dayExercises = exercisesByDate.getOrDefault(date, Collections.emptyList());

            List<ExerciseRecommendationCalendarDTO.ExerciseCalendarItem> exerciseItems;
            if(isCockpleRecommend){
                 exerciseItems = dayExercises.stream()
                        .map(exercise -> toRecommendationCalendarItemWithDistance(exercise, bookmarkStatus, mainAddr))
                        .sorted(Comparator.comparing(ExerciseRecommendationCalendarDTO.ExerciseCalendarItem::distance)
                                .thenComparing(ExerciseRecommendationCalendarDTO.ExerciseCalendarItem::startTime))
                        .toList();
            }else{
                exerciseItems = dayExercises.stream()
                        .map(exercise -> toRecommendationCalendarItem(exercise, bookmarkStatus))
                        .sorted(getFilterSortComparator(filterSortType, participantCountMap))
                        .toList();
            }

            dailyExercisesList.add(createRecommendationDailyExercises(date, exerciseItems));
        }

        return dailyExercisesList;
    }

    private ExerciseRecommendationCalendarDTO.WeeklyExercises createRecommendedWeeklyExercises(
            LocalDate weekStart,
            LocalDate weekEnd,
            List<ExerciseRecommendationCalendarDTO.DailyExercises> days) {

        return ExerciseRecommendationCalendarDTO.WeeklyExercises.builder()
                .weekStartDate(weekStart)
                .weekEndDate(weekEnd)
                .days(days)
                .build();
    }

    private ExerciseRecommendationCalendarDTO.DailyExercises createRecommendationDailyExercises(
            LocalDate date,
            List<ExerciseRecommendationCalendarDTO.ExerciseCalendarItem> exerciseItems) {

        return ExerciseRecommendationCalendarDTO.DailyExercises.builder()
                .date(date)
                .dayOfWeek(date.getDayOfWeek().name())
                .exercises(exerciseItems)
                .build();
    }

    private ExerciseRecommendationDTO.ExerciseItem toExerciseRecommendationItem(
            Exercise exercise, Map<Long, Boolean> bookmarkStatus) {

        Party party = exercise.getParty();

        return ExerciseRecommendationDTO.ExerciseItem.builder()
                .exerciseId(exercise.getId())
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .date(exercise.getDate())
                .dayOfWeek(exercise.getDate().getDayOfWeek().name())
                .startTime(exercise.getStartTime())
                .endTime(exercise.getEndTime())
                .buildingName(exercise.getExerciseAddr().getBuildingName())
                .profileImageUrl(getImageUrl(party.getPartyImg()))
                .isBookmarked(bookmarkStatus.getOrDefault(exercise.getId(), false))
                .build();
    }

    private ExerciseRecommendationCalendarDTO.ExerciseCalendarItem toRecommendationCalendarItemWithDistance(
            Exercise exercise, Map<Long, Boolean> bookmarkStatus, MemberAddr mainAddr) {

        Double distance = calculateDistance(mainAddr.getLatitude(), mainAddr.getLongitude(),
                exercise.getExerciseAddr().getLatitude(), exercise.getExerciseAddr().getLongitude());

        Party party = exercise.getParty();

        return ExerciseRecommendationCalendarDTO.ExerciseCalendarItem.builder()
                .exerciseId(exercise.getId())
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .buildingName(exercise.getExerciseAddr().getBuildingName())
                .startTime(exercise.getStartTime())
                .endTime(exercise.getEndTime())
                .profileImageUrl(getImageUrl(party.getPartyImg()))
                .isBookmarked(bookmarkStatus.getOrDefault(exercise.getId(), false))
                .distance(distance)
                .build();
    }

    private ExerciseRecommendationCalendarDTO.ExerciseCalendarItem toRecommendationCalendarItem(
            Exercise exercise, Map<Long, Boolean> bookmarkStatus) {

        Party party = exercise.getParty();

        return ExerciseRecommendationCalendarDTO.ExerciseCalendarItem.builder()
                .exerciseId(exercise.getId())
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .buildingName(exercise.getExerciseAddr().getBuildingName())
                .startTime(exercise.getStartTime())
                .endTime(exercise.getEndTime())
                .profileImageUrl(getImageUrl(party.getPartyImg()))
                .isBookmarked(bookmarkStatus.getOrDefault(exercise.getId(), false))
                .build();
    }

    private double calculateDistance(double latitude, double longitude, double latitude1, double longitude1) {
        final double R = 6371; // 지구 반지름 (km)

        double latDistance = Math.toRadians(latitude1 - latitude);
        double lonDistance = Math.toRadians(longitude1 - longitude);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(latitude1))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return (float) (R * c);
    }

    private Comparator<ExerciseRecommendationCalendarDTO.ExerciseCalendarItem> getFilterSortComparator(
            ExerciseRecommendationCalendarDTO.FilterSortType filterSortType,
            Map<Long, Integer> participantCountMap) {

        return switch (filterSortType.sortType()) {
            case LATEST ->
                    Comparator.comparing(ExerciseRecommendationCalendarDTO.ExerciseCalendarItem::startTime);

            case POPULARITY ->
                    Comparator.comparing(
                            (ExerciseRecommendationCalendarDTO.ExerciseCalendarItem item) ->
                                    participantCountMap.getOrDefault(item.exerciseId(), 0)
                    ).reversed()
                    .thenComparing(ExerciseRecommendationCalendarDTO.ExerciseCalendarItem::startTime);
        };
    }
}
