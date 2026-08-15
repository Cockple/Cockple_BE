package umc.cockple.demo.domain.exercise.service.support.assembler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseRecommendationCalendarResult;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseRecommendationResult;
import umc.cockple.demo.domain.exercise.service.support.ExerciseDistanceCalculator;
import umc.cockple.demo.domain.exercise.enums.MyPartyExerciseOrderType;
import umc.cockple.demo.domain.file.service.ImageUrlResolver;
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
public class ExerciseRecommendationResultAssembler {

    private final ImageUrlResolver imageUrlResolver;
    private final ExerciseDistanceCalculator exerciseDistanceCalculator;

    public ExerciseRecommendationResult toExerciseRecommendationResult(
            List<Exercise> finalExercises, Map<Long, Boolean> bookmarkStatus) {

        List<ExerciseRecommendationResult.ExerciseItem> exercises = finalExercises.stream()
                .map(exercise -> toExerciseRecommendationItem(exercise, bookmarkStatus))
                .toList();

        return ExerciseRecommendationResult.builder()
                .totalExercises(finalExercises.size())
                .exercises(exercises)
                .build();
    }

    public ExerciseRecommendationCalendarResult toRecommendationCalendarResult(
            List<Exercise> exercises,
            Map<Long, Boolean> bookmarkStatus,
            Map<Long, Integer> participantCountMap,
            MemberAddr mainAddr,
            LocalDate start,
            LocalDate end,
            Boolean isCockpleRecommend,
            MyPartyExerciseOrderType sortType) {

        List<ExerciseRecommendationCalendarResult.WeeklyExercises> weeks
                = groupRecommendedExerciseByWeek(exercises, bookmarkStatus, participantCountMap, mainAddr
                , start, end, isCockpleRecommend, sortType);

        return ExerciseRecommendationCalendarResult.builder()
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

    private List<ExerciseRecommendationCalendarResult.WeeklyExercises> groupRecommendedExerciseByWeek(
            List<Exercise> exercises,
            Map<Long, Boolean> bookmarkStatus,
            Map<Long, Integer> participantCountMap,
            MemberAddr mainAddr,
            LocalDate start,
            LocalDate end,
            Boolean isCockpleRecommend,
            MyPartyExerciseOrderType sortType) {

        List<ExerciseRecommendationCalendarResult.WeeklyExercises> weeks = new ArrayList<>();

        for (LocalDate weekStart = getWeekStart(start); !weekStart.isAfter(end); weekStart = weekStart.plusWeeks(1)) {
            LocalDate weekEnd = weekStart.plusDays(6);

            List<Exercise> weekExercises = filterExercisesByWeek(exercises, weekStart, weekEnd);

            List<ExerciseRecommendationCalendarResult.DailyExercises> dailyExercisesList =
                    groupRecommendedExercisesByDate(weekExercises, weekStart, weekEnd, bookmarkStatus, participantCountMap, mainAddr, isCockpleRecommend, sortType);

            weeks.add(createRecommendedWeeklyExercises(weekStart, weekEnd, dailyExercisesList));
        }

        return weeks;
    }

    private List<ExerciseRecommendationCalendarResult.DailyExercises> groupRecommendedExercisesByDate(
            List<Exercise> weekExercises,
            LocalDate weekStart,
            LocalDate weekEnd,
            Map<Long, Boolean> bookmarkStatus,
            Map<Long, Integer> participantCountMap,
            MemberAddr mainAddr,
            Boolean isCockpleRecommend,
            MyPartyExerciseOrderType sortType) {

        Map<LocalDate, List<Exercise>> exercisesByDate = weekExercises.stream()
                .collect(Collectors.groupingBy(Exercise::getDate));

        List<ExerciseRecommendationCalendarResult.DailyExercises> dailyExercisesList = new ArrayList<>();

        for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
            List<Exercise> dayExercises = exercisesByDate.getOrDefault(date, Collections.emptyList());

            List<ExerciseRecommendationCalendarResult.ExerciseCalendarItem> exerciseItems;
            if(isCockpleRecommend){
                 exerciseItems = dayExercises.stream()
                        .map(exercise -> toRecommendationCalendarItemWithDistance(exercise, bookmarkStatus, mainAddr))
                        .sorted(Comparator.comparing(ExerciseRecommendationCalendarResult.ExerciseCalendarItem::distance)
                                .thenComparing(ExerciseRecommendationCalendarResult.ExerciseCalendarItem::startTime))
                        .toList();
            }else{
                exerciseItems = dayExercises.stream()
                        .map(exercise -> toRecommendationCalendarItem(exercise, bookmarkStatus))
                        .sorted(getFilterSortComparator(sortType, participantCountMap))
                        .toList();
            }

            dailyExercisesList.add(createRecommendationDailyExercises(date, exerciseItems));
        }

        return dailyExercisesList;
    }

    private ExerciseRecommendationCalendarResult.WeeklyExercises createRecommendedWeeklyExercises(
            LocalDate weekStart,
            LocalDate weekEnd,
            List<ExerciseRecommendationCalendarResult.DailyExercises> days) {

        return ExerciseRecommendationCalendarResult.WeeklyExercises.builder()
                .weekStartDate(weekStart)
                .weekEndDate(weekEnd)
                .days(days)
                .build();
    }

    private ExerciseRecommendationCalendarResult.DailyExercises createRecommendationDailyExercises(
            LocalDate date,
            List<ExerciseRecommendationCalendarResult.ExerciseCalendarItem> exerciseItems) {

        return ExerciseRecommendationCalendarResult.DailyExercises.builder()
                .date(date)
                .dayOfWeek(date.getDayOfWeek().name())
                .exercises(exerciseItems)
                .build();
    }

    private ExerciseRecommendationResult.ExerciseItem toExerciseRecommendationItem(
            Exercise exercise, Map<Long, Boolean> bookmarkStatus) {

        Party party = exercise.getParty();

        return ExerciseRecommendationResult.ExerciseItem.builder()
                .exerciseId(exercise.getId())
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .date(exercise.getDate())
                .dayOfWeek(exercise.getDate().getDayOfWeek().name())
                .startTime(exercise.getStartTime())
                .endTime(exercise.getEndTime())
                .buildingName(exercise.getExerciseAddr().getBuildingName())
                .profileImageUrl(imageUrlResolver.resolve(party.getPartyImg(), PartyImg::getImgKey))
                .bookmarked(bookmarkStatus.getOrDefault(exercise.getId(), false))
                .build();
    }

    private ExerciseRecommendationCalendarResult.ExerciseCalendarItem toRecommendationCalendarItemWithDistance(
            Exercise exercise, Map<Long, Boolean> bookmarkStatus, MemberAddr mainAddr) {

        Double distance = exerciseDistanceCalculator.calculate(mainAddr.getLatitude(), mainAddr.getLongitude(),
                exercise.getExerciseAddr().getLatitude(), exercise.getExerciseAddr().getLongitude());

        Party party = exercise.getParty();

        return ExerciseRecommendationCalendarResult.ExerciseCalendarItem.builder()
                .exerciseId(exercise.getId())
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .buildingName(exercise.getExerciseAddr().getBuildingName())
                .startTime(exercise.getStartTime())
                .endTime(exercise.getEndTime())
                .profileImageUrl(imageUrlResolver.resolve(party.getPartyImg(), PartyImg::getImgKey))
                .bookmarked(bookmarkStatus.getOrDefault(exercise.getId(), false))
                .distance(distance)
                .build();
    }

    private ExerciseRecommendationCalendarResult.ExerciseCalendarItem toRecommendationCalendarItem(
            Exercise exercise, Map<Long, Boolean> bookmarkStatus) {

        Party party = exercise.getParty();

        return ExerciseRecommendationCalendarResult.ExerciseCalendarItem.builder()
                .exerciseId(exercise.getId())
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .buildingName(exercise.getExerciseAddr().getBuildingName())
                .startTime(exercise.getStartTime())
                .endTime(exercise.getEndTime())
                .profileImageUrl(imageUrlResolver.resolve(party.getPartyImg(), PartyImg::getImgKey))
                .bookmarked(bookmarkStatus.getOrDefault(exercise.getId(), false))
                .build();
    }

    private Comparator<ExerciseRecommendationCalendarResult.ExerciseCalendarItem> getFilterSortComparator(
            MyPartyExerciseOrderType sortType,
            Map<Long, Integer> participantCountMap) {

        return switch (sortType) {
            case LATEST ->
                    Comparator.comparing(ExerciseRecommendationCalendarResult.ExerciseCalendarItem::startTime);

            case POPULARITY ->
                    Comparator.comparing(
                            (ExerciseRecommendationCalendarResult.ExerciseCalendarItem item) ->
                                    participantCountMap.getOrDefault(item.exerciseId(), 0)
                    ).reversed()
                    .thenComparing(ExerciseRecommendationCalendarResult.ExerciseCalendarItem::startTime);
        };
    }
}
