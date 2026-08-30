package umc.cockple.demo.domain.exercise.service.support.assembler;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.service.query.result.PartyExerciseCalendarResult;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Gender;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PartyExerciseCalendarResultAssembler {

    public PartyExerciseCalendarResult toEmptyPartyCalendarResult(
            LocalDate start,
            LocalDate end,
            Boolean isMember,
            Party party) {

        return PartyExerciseCalendarResult.builder()
                .startDate(start)
                .endDate(end)
                .member(isMember)
                .partyName(party.getPartyName())
                .weeks(Collections.emptyList())
                .build();
    }

    public PartyExerciseCalendarResult toPartyCalendarResult(
            List<Exercise> exercises,
            LocalDate start,
            LocalDate end,
            Boolean isMember,
            Party party,
            Map<Long, Integer> participantCounts,
            Map<Long, Boolean> bookmarkStatus,
            Map<Long, Boolean> participatingStatus) {

        PartyLevelCache levelCache = createPartyLevelCache(party);

        List<PartyExerciseCalendarResult.WeeklyExercises> weeks
                = groupPartyExerciseByWeek(exercises, levelCache, participantCounts, bookmarkStatus, start, end, participatingStatus);

        return PartyExerciseCalendarResult.builder()
                .startDate(start)
                .endDate(end)
                .member(isMember)
                .partyName(party.getPartyName())
                .weeks(weeks)
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

    private List<PartyExerciseCalendarResult.WeeklyExercises> groupPartyExerciseByWeek(
            List<Exercise> exercises,
            PartyLevelCache levelCache,
            Map<Long, Integer> participantCounts,
            Map<Long, Boolean> bookmarkStatus,
            LocalDate start,
            LocalDate end,
            Map<Long, Boolean> participatingStatus) {

        List<PartyExerciseCalendarResult.WeeklyExercises> weeks = new ArrayList<>();

        for (LocalDate weekStart = getWeekStart(start); !weekStart.isAfter(end); weekStart = weekStart.plusWeeks(1)) {
            LocalDate weekEnd = weekStart.plusDays(6);

            List<Exercise> weekExercises = filterExercisesByWeek(exercises, weekStart, weekEnd);

            List<PartyExerciseCalendarResult.DailyExercises> dailyExercisesList =
                    groupPartyExerciseByDate(weekExercises, weekStart, weekEnd, levelCache, participantCounts, bookmarkStatus, participatingStatus);

            weeks.add(createPartyWeeklyExercises(weekStart, weekEnd, dailyExercisesList));
        }

        return weeks;
    }

    private List<PartyExerciseCalendarResult.DailyExercises> groupPartyExerciseByDate(
            List<Exercise> weekExercises,
            LocalDate weekStart,
            LocalDate weekEnd,
            PartyLevelCache levelCache,
            Map<Long, Integer> participantCounts,
            Map<Long, Boolean> bookmarkStatus,
            Map<Long, Boolean> participatingStatus) {

        Map<LocalDate, List<Exercise>> exercisesByDate = weekExercises.stream()
                .collect(Collectors.groupingBy(Exercise::getDate));

        List<PartyExerciseCalendarResult.DailyExercises> dailyExercisesList = new ArrayList<>();

        for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
            List<Exercise> dayExercises = exercisesByDate.getOrDefault(date, Collections.emptyList());

            List<PartyExerciseCalendarResult.ExerciseCalendarItem> exerciseItems = dayExercises.stream()
                    .map(exercise -> toPartyCalendarItem(exercise, levelCache, participantCounts, bookmarkStatus, participatingStatus))
                    .toList();

            dailyExercisesList.add(createPartyDailyExercises(date, exerciseItems));
        }

        return dailyExercisesList;
    }

    private PartyExerciseCalendarResult.WeeklyExercises createPartyWeeklyExercises(
            LocalDate weekStart,
            LocalDate weekEnd,
            List<PartyExerciseCalendarResult.DailyExercises> days) {

        return PartyExerciseCalendarResult.WeeklyExercises.builder()
                .weekStartDate(weekStart)
                .weekEndDate(weekEnd)
                .days(days)
                .build();
    }

    private PartyExerciseCalendarResult.DailyExercises createPartyDailyExercises(
            LocalDate date,
            List<PartyExerciseCalendarResult.ExerciseCalendarItem> exerciseItems) {

        return PartyExerciseCalendarResult.DailyExercises.builder()
                .date(date)
                .exercises(exerciseItems)
                .build();
    }

    private PartyExerciseCalendarResult.ExerciseCalendarItem toPartyCalendarItem(
            Exercise exercise,
            PartyLevelCache levelCache,
            Map<Long, Integer> participantCounts,
            Map<Long, Boolean> bookmarkStatus,
            Map<Long, Boolean> participatingStatus) {

        Integer currentParticipants = participantCounts.getOrDefault(exercise.getId(), 0);

        return PartyExerciseCalendarResult.ExerciseCalendarItem.builder()
                .exerciseId(exercise.getId())
                .bookmarked(bookmarkStatus.getOrDefault(exercise.getId(), false))
                .startTime(exercise.getStartTime())
                .endTime(exercise.getEndTime())
                .buildingName(exercise.getExerciseAddr().getBuildingName())
                .femaleLevel(levelCache.femaleLevel())
                .maleLevel(levelCache.maleLevel())
                .currentParticipants(currentParticipants)
                .maxCapacity(exercise.getMaxCapacity())
                .participating(participatingStatus.getOrDefault(exercise.getId(), false))
                .build();
    }

    private record PartyLevelCache(
            List<String> femaleLevel,
            List<String> maleLevel
    ) {
    }
}
