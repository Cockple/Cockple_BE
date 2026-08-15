package umc.cockple.demo.domain.exercise.service.support.assembler;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.enums.MyPartyExerciseOrderType;
import umc.cockple.demo.domain.exercise.service.query.result.MyExerciseCalendarResult;
import umc.cockple.demo.domain.exercise.service.query.result.MyExerciseListResult;
import umc.cockple.demo.domain.exercise.service.query.result.MyPartyExerciseCalendarResult;
import umc.cockple.demo.domain.exercise.service.query.result.MyPartyExerciseResult;
import umc.cockple.demo.domain.file.service.ImageUrlResolver;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyImg;
import umc.cockple.demo.global.enums.Gender;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ExerciseMyResultAssembler {

    private final ImageUrlResolver imageUrlResolver;

    public MyExerciseCalendarResult toEmptyMyCalendarResult(LocalDate start, LocalDate end) {
        return MyExerciseCalendarResult.builder()
                .startDate(start)
                .endDate(end)
                .weeks(Collections.emptyList())
                .build();
    }

    public MyExerciseCalendarResult toMyCalendarResult(List<Exercise> exercises, LocalDate start, LocalDate end) {

        List<MyExerciseCalendarResult.WeeklyExercises> weeks = groupMyExerciseByWeek(exercises, start, end);

        return MyExerciseCalendarResult.builder()
                .startDate(start)
                .endDate(end)
                .weeks(weeks)
                .build();
    }

    public MyPartyExerciseResult toEmptyMyPartyExerciseResult() {
        return MyPartyExerciseResult.builder()
                .totalExercises(0)
                .exercises(List.of())
                .build();
    }

    public MyPartyExerciseResult toMyPartyExerciseResult(List<Exercise> recentExercises) {

        List<MyPartyExerciseResult.ExerciseItem> exercises = recentExercises.stream()
                .map(this::toPartyExerciseItem)
                .toList();

        return MyPartyExerciseResult.builder()
                .totalExercises(recentExercises.size())
                .exercises(exercises)
                .build();
    }

    public MyPartyExerciseCalendarResult toEmptyMyPartyCalendarResult(LocalDate start, LocalDate end) {
        return MyPartyExerciseCalendarResult.builder()
                .startDate(start)
                .endDate(end)
                .weeks(Collections.emptyList())
                .build();
    }

    public MyPartyExerciseCalendarResult toMyPartyCalendarResult(
            List<Exercise> exercises,
            LocalDate start,
            LocalDate end,
            Map<Long, Boolean> bookmarkStatus,
            MyPartyExerciseOrderType orderType,
            Map<Long, Integer> participantCounts) {

        List<MyPartyExerciseCalendarResult.WeeklyExercises> weeks =
                groupMyPartyExerciseByWeek(exercises, start, end, bookmarkStatus, orderType, participantCounts);

        return MyPartyExerciseCalendarResult.builder()
                .startDate(start)
                .endDate(end)
                .weeks(weeks)
                .build();
    }

    public MyExerciseListResult toEmptyMyExerciseListResult() {
        return MyExerciseListResult.builder()
                .totalCount(0)
                .hasNext(false)
                .exercises(List.of())
                .build();
    }

    public MyExerciseListResult toMyExerciseListResult(
            Slice<Exercise> exerciseSlice,
            Map<Long, Integer> participantCountMap,
            Map<Long, Boolean> bookmarkStatus,
            Map<Long, Boolean> isCompletedMap) {

        List<MyExerciseListResult.ExerciseItem> exercises = exerciseSlice.getContent().stream()
                .map(exercise -> toMyExerciseItem(exercise, participantCountMap, bookmarkStatus, isCompletedMap))
                .toList();

        return MyExerciseListResult.builder()
                .totalCount(exercises.size())
                .hasNext(exerciseSlice.hasNext())
                .exercises(exercises)
                .build();
    }

    private PartyLevelCache createPartyLevelCache(Party party) {
        List<String> femaleLevel = extractLevelsByGender(party, Gender.FEMALE);
        List<String> maleLevel = extractLevelsByGender(party, Gender.MALE);

        return new PartyLevelCache(femaleLevel, maleLevel);
    }

    private List<String> extractLevelsByGender(Party party, Gender gender) {
        List<String> levelList = party.getLevels().stream()
                .filter(l -> l.getGender() == gender)
                .map(l -> l.getLevel().getKoreanName())
                .toList();

        return levelList.isEmpty() ? null : levelList;
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

    private List<MyExerciseCalendarResult.WeeklyExercises> groupMyExerciseByWeek(
            List<Exercise> exercises, LocalDate start, LocalDate end) {

        List<MyExerciseCalendarResult.WeeklyExercises> weeks = new ArrayList<>();

        for (LocalDate weekStart = getWeekStart(start); !weekStart.isAfter(end); weekStart = weekStart.plusWeeks(1)) {
            LocalDate weekEnd = weekStart.plusDays(6);

            List<Exercise> weekExercises = filterExercisesByWeek(exercises, weekStart, weekEnd);

            List<MyExerciseCalendarResult.DailyExercises> dailyExercisesList =
                    groupMyExerciseByDate(weekExercises, weekStart, weekEnd);

            weeks.add(createMyWeeklyExercises(weekStart, weekEnd, dailyExercisesList));
        }

        return weeks;
    }

    private List<MyPartyExerciseCalendarResult.WeeklyExercises> groupMyPartyExerciseByWeek(
            List<Exercise> exercises,
            LocalDate start,
            LocalDate end,
            Map<Long, Boolean> bookmarkStatus,
            MyPartyExerciseOrderType orderType,
            Map<Long, Integer> participantCounts) {

        List<MyPartyExerciseCalendarResult.WeeklyExercises> weeks = new ArrayList<>();

        for (LocalDate weekStart = getWeekStart(start); !weekStart.isAfter(end); weekStart = weekStart.plusWeeks(1)) {
            LocalDate weekEnd = weekStart.plusDays(6);

            List<Exercise> weekExercises = filterExercisesByWeek(exercises, weekStart, weekEnd);

            List<MyPartyExerciseCalendarResult.DailyExercises> dailyExercisesList =
                    groupMyPartyExercisesByDate(weekExercises, weekStart, weekEnd, bookmarkStatus, orderType, participantCounts);

            weeks.add(createMyPartyWeeklyExercises(weekStart, weekEnd, dailyExercisesList));
        }

        return weeks;
    }

    private List<MyExerciseCalendarResult.DailyExercises> groupMyExerciseByDate(
            List<Exercise> weekExercises,
            LocalDate weekStart,
            LocalDate weekEnd) {

        Map<LocalDate, List<Exercise>> exercisesByDate = weekExercises.stream()
                .collect(Collectors.groupingBy(Exercise::getDate));

        List<MyExerciseCalendarResult.DailyExercises> dailyExercisesList = new ArrayList<>();

        for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
            List<Exercise> dayExercises = exercisesByDate.getOrDefault(date, Collections.emptyList());

            List<MyExerciseCalendarResult.ExerciseCalendarItem> exerciseItems = dayExercises.stream()
                    .map(this::toMyCalendarItem)
                    .toList();

            dailyExercisesList.add(createMyDailyExercises(date, exerciseItems));
        }

        return dailyExercisesList;
    }

    private List<MyPartyExerciseCalendarResult.DailyExercises> groupMyPartyExercisesByDate(
            List<Exercise> weekExercises,
            LocalDate weekStart,
            LocalDate weekEnd,
            Map<Long, Boolean> bookmarkStatus,
            MyPartyExerciseOrderType orderType,
            Map<Long, Integer> participantCounts) {

        Map<LocalDate, List<Exercise>> exercisesByDate = weekExercises.stream()
                .collect(Collectors.groupingBy(Exercise::getDate));

        List<MyPartyExerciseCalendarResult.DailyExercises> dailyExercisesList = new ArrayList<>();

        for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
            List<Exercise> dayExercises = exercisesByDate.getOrDefault(date, Collections.emptyList());

            if (Objects.requireNonNull(orderType) == MyPartyExerciseOrderType.LATEST) {
                dayExercises.sort(Comparator.comparing(Exercise::getStartTime));
            } else if (orderType == MyPartyExerciseOrderType.POPULARITY) {
                dayExercises.sort(Comparator.comparingInt((Exercise e) -> participantCounts.getOrDefault(e.getId(), 0)).reversed());
            }

            List<MyPartyExerciseCalendarResult.ExerciseCalendarItem> exerciseItems = dayExercises.stream()
                    .map(exercise -> toMyPartyCalendarItem(exercise, bookmarkStatus, participantCounts))
                    .toList();

            dailyExercisesList.add(createMyPartyDailyExercises(date, exerciseItems));
        }

        return dailyExercisesList;
    }

    private MyExerciseCalendarResult.WeeklyExercises createMyWeeklyExercises(
            LocalDate weekStart,
            LocalDate weekEnd,
            List<MyExerciseCalendarResult.DailyExercises> days) {

        return MyExerciseCalendarResult.WeeklyExercises.builder()
                .weekStartDate(weekStart)
                .weekEndDate(weekEnd)
                .days(days)
                .build();
    }

    private MyPartyExerciseCalendarResult.WeeklyExercises createMyPartyWeeklyExercises(
            LocalDate weekStart,
            LocalDate weekEnd,
            List<MyPartyExerciseCalendarResult.DailyExercises> days) {

        return MyPartyExerciseCalendarResult.WeeklyExercises.builder()
                .weekStartDate(weekStart)
                .weekEndDate(weekEnd)
                .days(days)
                .build();
    }

    private MyExerciseCalendarResult.DailyExercises createMyDailyExercises(
            LocalDate date,
            List<MyExerciseCalendarResult.ExerciseCalendarItem> exerciseItems) {

        return MyExerciseCalendarResult.DailyExercises.builder()
                .date(date)
                .dayOfWeek(date.getDayOfWeek().name())
                .exercises(exerciseItems)
                .build();
    }

    private MyPartyExerciseCalendarResult.DailyExercises createMyPartyDailyExercises(
            LocalDate date,
            List<MyPartyExerciseCalendarResult.ExerciseCalendarItem> exerciseItems) {

        return MyPartyExerciseCalendarResult.DailyExercises.builder()
                .date(date)
                .dayOfWeek(date.getDayOfWeek().name())
                .exercises(exerciseItems)
                .build();
    }

    private MyExerciseCalendarResult.ExerciseCalendarItem toMyCalendarItem(Exercise exercise) {

        Party party = exercise.getParty();

        return MyExerciseCalendarResult.ExerciseCalendarItem.builder()
                .exerciseId(exercise.getId())
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .buildingName(exercise.getExerciseAddr().getBuildingName())
                .startTime(exercise.getStartTime())
                .endTime(exercise.getEndTime())
                .profileImageUrl(imageUrlResolver.resolve(party.getPartyImg(), PartyImg::getImgKey))
                .build();
    }

    private MyPartyExerciseResult.ExerciseItem toPartyExerciseItem(Exercise exercise) {
        Party party = exercise.getParty();

        return MyPartyExerciseResult.ExerciseItem.builder()
                .exerciseId(exercise.getId())
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .buildingName(exercise.getExerciseAddr().getBuildingName())
                .date(exercise.getDate())
                .dayOfWeek(exercise.getDate().getDayOfWeek().name())
                .startTime(exercise.getStartTime())
                .profileImageUrl(imageUrlResolver.resolve(party.getPartyImg(), PartyImg::getImgKey))
                .build();
    }

    private MyPartyExerciseCalendarResult.ExerciseCalendarItem toMyPartyCalendarItem(
            Exercise exercise, Map<Long, Boolean> bookmarkStatus, Map<Long, Integer> participantCounts) {

        Party party = exercise.getParty();

        return MyPartyExerciseCalendarResult.ExerciseCalendarItem.builder()
                .exerciseId(exercise.getId())
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .buildingName(exercise.getExerciseAddr().getBuildingName())
                .startTime(exercise.getStartTime())
                .endTime(exercise.getEndTime())
                .profileImageUrl(imageUrlResolver.resolve(party.getPartyImg(), PartyImg::getImgKey))
                .bookmarked(bookmarkStatus.getOrDefault(exercise.getId(), false))
                .nowCapacity(participantCounts.getOrDefault(exercise.getId(), 0))
                .build();
    }

    private MyExerciseListResult.ExerciseItem toMyExerciseItem(
            Exercise exercise,
            Map<Long, Integer> participantCountMap,
            Map<Long, Boolean> bookmarkStatus,
            Map<Long, Boolean> isCompletedMap) {

        Party party = exercise.getParty();
        PartyLevelCache levelCache = createPartyLevelCache(party);

        return MyExerciseListResult.ExerciseItem.builder()
                .exerciseId(exercise.getId())
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .bookmarked(bookmarkStatus.getOrDefault(exercise.getId(), false))
                .date(exercise.getDate())
                .dayOfWeek(exercise.getDate().getDayOfWeek().name())
                .buildingName(exercise.getExerciseAddr().getBuildingName())
                .startTime(exercise.getStartTime())
                .endTime(exercise.getEndTime())
                .femaleLevel(levelCache.femaleLevel())
                .maleLevel(levelCache.maleLevel())
                .currentParticipants(participantCountMap.getOrDefault(exercise.getId(), 0))
                .maxCapacity(exercise.getMaxCapacity())
                .completed(isCompletedMap.getOrDefault(exercise.getId(), false))
                .partyGuestInviteAccept(exercise.getPartyGuestAccept())
                .build();
    }

    private record PartyLevelCache(
            List<String> femaleLevel,
            List<String> maleLevel
    ) {
    }
}
