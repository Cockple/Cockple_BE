package umc.cockple.demo.domain.exercise.converter.query;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.dto.MyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.dto.MyExerciseListDTO;
import umc.cockple.demo.domain.exercise.dto.MyPartyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.dto.MyPartyExerciseDTO;
import umc.cockple.demo.domain.exercise.enums.MyPartyExerciseOrderType;
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
public class ExerciseMyQueryMapper {

    private final ImageUrlResolver imageUrlResolver;

    public MyExerciseCalendarDTO.Response toEmptyMyCalendarResponse(LocalDate start, LocalDate end) {
        return MyExerciseCalendarDTO.Response.builder()
                .startDate(start)
                .endDate(end)
                .weeks(Collections.emptyList())
                .build();
    }

    public MyExerciseCalendarDTO.Response toMyCalendarResponse(List<Exercise> exercises, LocalDate start, LocalDate end) {

        List<MyExerciseCalendarDTO.WeeklyExercises> weeks = groupMyExerciseByWeek(exercises, start, end);

        return MyExerciseCalendarDTO.Response.builder()
                .startDate(start)
                .endDate(end)
                .weeks(weeks)
                .build();
    }

    public MyPartyExerciseDTO.Response toEmptyMyPartyExerciseResponse() {
        return MyPartyExerciseDTO.Response.builder()
                .totalExercises(0)
                .exercises(List.of())
                .build();
    }

    public MyPartyExerciseDTO.Response toMyPartyExerciseDTO(List<Exercise> recentExercises) {

        List<MyPartyExerciseDTO.Exercises> exercises = recentExercises.stream()
                .map(this::toPartyExerciseItem)
                .toList();

        return MyPartyExerciseDTO.Response.builder()
                .totalExercises(recentExercises.size())
                .exercises(exercises)
                .build();
    }

    public MyPartyExerciseCalendarDTO.Response toEmptyMyPartyCalendarResponse(LocalDate start, LocalDate end) {
        return MyPartyExerciseCalendarDTO.Response.builder()
                .startDate(start)
                .endDate(end)
                .weeks(Collections.emptyList())
                .build();
    }

    public MyPartyExerciseCalendarDTO.Response toMyPartyCalendarResponse(
            List<Exercise> exercises,
            LocalDate start,
            LocalDate end,
            Map<Long, Boolean> bookmarkStatus,
            MyPartyExerciseOrderType orderType,
            Map<Long, Integer> participantCounts) {

        List<MyPartyExerciseCalendarDTO.WeeklyExercises> weeks =
                groupMyPartyExerciseByWeek(exercises, start, end, bookmarkStatus, orderType, participantCounts);

        return MyPartyExerciseCalendarDTO.Response.builder()
                .startDate(start)
                .endDate(end)
                .weeks(weeks)
                .build();
    }

    public MyExerciseListDTO.Response toEmptyMyExerciseList() {
        return MyExerciseListDTO.Response.builder()
                .totalCount(0)
                .hasNext(false)
                .exercises(List.of())
                .build();
    }

    public MyExerciseListDTO.Response toMyExerciseListResponse(
            Slice<Exercise> exerciseSlice,
            Map<Long, Integer> participantCountMap,
            Map<Long, Boolean> bookmarkStatus,
            Map<Long, Boolean> isCompletedMap) {

        List<MyExerciseListDTO.ExerciseItem> exercises = exerciseSlice.getContent().stream()
                .map(exercise -> toMyExerciseItem(exercise, participantCountMap, bookmarkStatus, isCompletedMap))
                .toList();

        return MyExerciseListDTO.Response.builder()
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

    private List<MyExerciseCalendarDTO.WeeklyExercises> groupMyExerciseByWeek(
            List<Exercise> exercises, LocalDate start, LocalDate end) {

        List<MyExerciseCalendarDTO.WeeklyExercises> weeks = new ArrayList<>();

        for (LocalDate weekStart = getWeekStart(start); !weekStart.isAfter(end); weekStart = weekStart.plusWeeks(1)) {
            LocalDate weekEnd = weekStart.plusDays(6);

            List<Exercise> weekExercises = filterExercisesByWeek(exercises, weekStart, weekEnd);

            List<MyExerciseCalendarDTO.DailyExercises> dailyExercisesList =
                    groupMyExerciseByDate(weekExercises, weekStart, weekEnd);

            weeks.add(createMyWeeklyExercises(weekStart, weekEnd, dailyExercisesList));
        }

        return weeks;
    }

    private List<MyPartyExerciseCalendarDTO.WeeklyExercises> groupMyPartyExerciseByWeek(
            List<Exercise> exercises,
            LocalDate start,
            LocalDate end,
            Map<Long, Boolean> bookmarkStatus,
            MyPartyExerciseOrderType orderType,
            Map<Long, Integer> participantCounts) {

        List<MyPartyExerciseCalendarDTO.WeeklyExercises> weeks = new ArrayList<>();

        for (LocalDate weekStart = getWeekStart(start); !weekStart.isAfter(end); weekStart = weekStart.plusWeeks(1)) {
            LocalDate weekEnd = weekStart.plusDays(6);

            List<Exercise> weekExercises = filterExercisesByWeek(exercises, weekStart, weekEnd);

            List<MyPartyExerciseCalendarDTO.DailyExercises> dailyExercisesList =
                    groupMyPartyExercisesByDate(weekExercises, weekStart, weekEnd, bookmarkStatus, orderType, participantCounts);

            weeks.add(createMyPartyWeeklyExercises(weekStart, weekEnd, dailyExercisesList));
        }

        return weeks;
    }

    private List<MyExerciseCalendarDTO.DailyExercises> groupMyExerciseByDate(
            List<Exercise> weekExercises,
            LocalDate weekStart,
            LocalDate weekEnd) {

        Map<LocalDate, List<Exercise>> exercisesByDate = weekExercises.stream()
                .collect(Collectors.groupingBy(Exercise::getDate));

        List<MyExerciseCalendarDTO.DailyExercises> dailyExercisesList = new ArrayList<>();

        for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
            List<Exercise> dayExercises = exercisesByDate.getOrDefault(date, Collections.emptyList());

            List<MyExerciseCalendarDTO.ExerciseCalendarItem> exerciseItems = dayExercises.stream()
                    .map(this::toMyCalendarItem)
                    .toList();

            dailyExercisesList.add(createMyDailyExercises(date, exerciseItems));
        }

        return dailyExercisesList;
    }

    private List<MyPartyExerciseCalendarDTO.DailyExercises> groupMyPartyExercisesByDate(
            List<Exercise> weekExercises,
            LocalDate weekStart,
            LocalDate weekEnd,
            Map<Long, Boolean> bookmarkStatus,
            MyPartyExerciseOrderType orderType,
            Map<Long, Integer> participantCounts) {

        Map<LocalDate, List<Exercise>> exercisesByDate = weekExercises.stream()
                .collect(Collectors.groupingBy(Exercise::getDate));

        List<MyPartyExerciseCalendarDTO.DailyExercises> dailyExercisesList = new ArrayList<>();

        for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
            List<Exercise> dayExercises = exercisesByDate.getOrDefault(date, Collections.emptyList());

            if (Objects.requireNonNull(orderType) == MyPartyExerciseOrderType.LATEST) {
                dayExercises.sort(Comparator.comparing(Exercise::getStartTime));
            } else if (orderType == MyPartyExerciseOrderType.POPULARITY) {
                dayExercises.sort(Comparator.comparingInt((Exercise e) -> participantCounts.getOrDefault(e.getId(), 0)).reversed());
            }

            List<MyPartyExerciseCalendarDTO.ExerciseCalendarItem> exerciseItems = dayExercises.stream()
                    .map(exercise -> toMyPartyCalendarItem(exercise, bookmarkStatus, participantCounts))
                    .toList();

            dailyExercisesList.add(createMyPartyDailyExercises(date, exerciseItems));
        }

        return dailyExercisesList;
    }

    private MyExerciseCalendarDTO.WeeklyExercises createMyWeeklyExercises(
            LocalDate weekStart,
            LocalDate weekEnd,
            List<MyExerciseCalendarDTO.DailyExercises> days) {

        return MyExerciseCalendarDTO.WeeklyExercises.builder()
                .weekStartDate(weekStart)
                .weekEndDate(weekEnd)
                .days(days)
                .build();
    }

    private MyPartyExerciseCalendarDTO.WeeklyExercises createMyPartyWeeklyExercises(
            LocalDate weekStart,
            LocalDate weekEnd,
            List<MyPartyExerciseCalendarDTO.DailyExercises> days) {

        return MyPartyExerciseCalendarDTO.WeeklyExercises.builder()
                .weekStartDate(weekStart)
                .weekEndDate(weekEnd)
                .days(days)
                .build();
    }

    private MyExerciseCalendarDTO.DailyExercises createMyDailyExercises(
            LocalDate date,
            List<MyExerciseCalendarDTO.ExerciseCalendarItem> exerciseItems) {

        return MyExerciseCalendarDTO.DailyExercises.builder()
                .date(date)
                .dayOfWeek(date.getDayOfWeek().name())
                .exercises(exerciseItems)
                .build();
    }

    private MyPartyExerciseCalendarDTO.DailyExercises createMyPartyDailyExercises(
            LocalDate date,
            List<MyPartyExerciseCalendarDTO.ExerciseCalendarItem> exerciseItems) {

        return MyPartyExerciseCalendarDTO.DailyExercises.builder()
                .date(date)
                .dayOfWeek(date.getDayOfWeek().name())
                .exercises(exerciseItems)
                .build();
    }

    private MyExerciseCalendarDTO.ExerciseCalendarItem toMyCalendarItem(Exercise exercise) {

        Party party = exercise.getParty();

        return MyExerciseCalendarDTO.ExerciseCalendarItem.builder()
                .exerciseId(exercise.getId())
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .buildingName(exercise.getExerciseAddr().getBuildingName())
                .startTime(exercise.getStartTime())
                .endTime(exercise.getEndTime())
                .profileImageUrl(imageUrlResolver.resolve(party.getPartyImg(), PartyImg::getImgKey))
                .build();
    }

    private MyPartyExerciseDTO.Exercises toPartyExerciseItem(Exercise exercise) {
        Party party = exercise.getParty();

        return MyPartyExerciseDTO.Exercises.builder()
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

    private MyPartyExerciseCalendarDTO.ExerciseCalendarItem toMyPartyCalendarItem(
            Exercise exercise, Map<Long, Boolean> bookmarkStatus, Map<Long, Integer> participantCounts) {

        Party party = exercise.getParty();

        return MyPartyExerciseCalendarDTO.ExerciseCalendarItem.builder()
                .exerciseId(exercise.getId())
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .buildingName(exercise.getExerciseAddr().getBuildingName())
                .startTime(exercise.getStartTime())
                .endTime(exercise.getEndTime())
                .profileImageUrl(imageUrlResolver.resolve(party.getPartyImg(), PartyImg::getImgKey))
                .isBookmarked(bookmarkStatus.getOrDefault(exercise.getId(), false))
                .nowCapacity(participantCounts.getOrDefault(exercise.getId(), 0))
                .build();
    }

    private MyExerciseListDTO.ExerciseItem toMyExerciseItem(
            Exercise exercise,
            Map<Long, Integer> participantCountMap,
            Map<Long, Boolean> bookmarkStatus,
            Map<Long, Boolean> isCompletedMap) {

        Party party = exercise.getParty();
        PartyLevelCache levelCache = createPartyLevelCache(party);

        return MyExerciseListDTO.ExerciseItem.builder()
                .exerciseId(exercise.getId())
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .isBookmarked(bookmarkStatus.getOrDefault(exercise.getId(), false))
                .date(exercise.getDate())
                .dayOfWeek(exercise.getDate().getDayOfWeek().name())
                .buildingName(exercise.getExerciseAddr().getBuildingName())
                .startTime(exercise.getStartTime())
                .endTime(exercise.getEndTime())
                .femaleLevel(levelCache.femaleLevel())
                .maleLevel(levelCache.maleLevel())
                .currentParticipants(participantCountMap.getOrDefault(exercise.getId(), 0))
                .maxCapacity(exercise.getMaxCapacity())
                .isCompleted(isCompletedMap.getOrDefault(exercise.getId(), false))
                .partyGuestInviteAccept(exercise.getPartyGuestAccept())
                .build();
    }

    private record PartyLevelCache(
            List<String> femaleLevel,
            List<String> maleLevel
    ) {
    }
}
