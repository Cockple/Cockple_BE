package umc.cockple.demo.domain.exercise.converter;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.dto.PartyExerciseCalendarDTO;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Gender;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PartyExerciseCalendarMapper {

    public PartyExerciseCalendarDTO.Response toEmptyPartyCalendarResponse(
            LocalDate start,
            LocalDate end,
            Boolean isMember,
            Party party) {

        return PartyExerciseCalendarDTO.Response.builder()
                .startDate(start)
                .endDate(end)
                .isMember(isMember)
                .partyName(party.getPartyName())
                .weeks(Collections.emptyList())
                .build();
    }

    public PartyExerciseCalendarDTO.Response toPartyCalendarResponse(
            List<Exercise> exercises,
            LocalDate start,
            LocalDate end,
            Boolean isMember,
            Party party,
            Map<Long, Integer> participantCounts,
            Map<Long, Boolean> bookmarkStatus,
            Map<Long, Boolean> participatingStatus) {

        PartyLevelCache levelCache = createPartyLevelCache(party);

        List<PartyExerciseCalendarDTO.WeeklyExercises> weeks
                = groupPartyExerciseByWeek(exercises, levelCache, participantCounts, bookmarkStatus, start, end, participatingStatus);

        return PartyExerciseCalendarDTO.Response.builder()
                .startDate(start)
                .endDate(end)
                .isMember(isMember)
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

    private List<PartyExerciseCalendarDTO.WeeklyExercises> groupPartyExerciseByWeek(
            List<Exercise> exercises,
            PartyLevelCache levelCache,
            Map<Long, Integer> participantCounts,
            Map<Long, Boolean> bookmarkStatus,
            LocalDate start,
            LocalDate end,
            Map<Long, Boolean> participatingStatus) {

        List<PartyExerciseCalendarDTO.WeeklyExercises> weeks = new ArrayList<>();

        for (LocalDate weekStart = getWeekStart(start); !weekStart.isAfter(end); weekStart = weekStart.plusWeeks(1)) {
            LocalDate weekEnd = weekStart.plusDays(6);

            List<Exercise> weekExercises = filterExercisesByWeek(exercises, weekStart, weekEnd);

            List<PartyExerciseCalendarDTO.DailyExercises> dailyExercisesList =
                    groupPartyExerciseByDate(weekExercises, weekStart, weekEnd, levelCache, participantCounts, bookmarkStatus, participatingStatus);

            weeks.add(createPartyWeeklyExercises(weekStart, weekEnd, dailyExercisesList));
        }

        return weeks;
    }

    private List<PartyExerciseCalendarDTO.DailyExercises> groupPartyExerciseByDate(
            List<Exercise> weekExercises,
            LocalDate weekStart,
            LocalDate weekEnd,
            PartyLevelCache levelCache,
            Map<Long, Integer> participantCounts,
            Map<Long, Boolean> bookmarkStatus,
            Map<Long, Boolean> participatingStatus) {

        Map<LocalDate, List<Exercise>> exercisesByDate = weekExercises.stream()
                .collect(Collectors.groupingBy(Exercise::getDate));

        List<PartyExerciseCalendarDTO.DailyExercises> dailyExercisesList = new ArrayList<>();

        for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
            List<Exercise> dayExercises = exercisesByDate.getOrDefault(date, Collections.emptyList());

            List<PartyExerciseCalendarDTO.ExerciseCalendarItem> exerciseItems = dayExercises.stream()
                    .map(exercise -> toPartyCalendarItem(exercise, levelCache, participantCounts, bookmarkStatus, participatingStatus))
                    .toList();

            dailyExercisesList.add(createPartyDailyExercises(date, exerciseItems));
        }

        return dailyExercisesList;
    }

    private PartyExerciseCalendarDTO.WeeklyExercises createPartyWeeklyExercises(
            LocalDate weekStart,
            LocalDate weekEnd,
            List<PartyExerciseCalendarDTO.DailyExercises> days) {

        return PartyExerciseCalendarDTO.WeeklyExercises.builder()
                .weekStartDate(weekStart)
                .weekEndDate(weekEnd)
                .days(days)
                .build();
    }

    private PartyExerciseCalendarDTO.DailyExercises createPartyDailyExercises(
            LocalDate date,
            List<PartyExerciseCalendarDTO.ExerciseCalendarItem> exerciseItems) {

        return PartyExerciseCalendarDTO.DailyExercises.builder()
                .date(date)
                .dayOfWeek(date.getDayOfWeek().name())
                .exercises(exerciseItems)
                .build();
    }

    private PartyExerciseCalendarDTO.ExerciseCalendarItem toPartyCalendarItem(
            Exercise exercise,
            PartyLevelCache levelCache,
            Map<Long, Integer> participantCounts,
            Map<Long, Boolean> bookmarkStatus,
            Map<Long, Boolean> participatingStatus) {

        Integer currentParticipants = participantCounts.getOrDefault(exercise.getId(), 0);

        return PartyExerciseCalendarDTO.ExerciseCalendarItem.builder()
                .exerciseId(exercise.getId())
                .isBookmarked(bookmarkStatus.getOrDefault(exercise.getId(), false))
                .startTime(exercise.getStartTime())
                .endTime(exercise.getEndTime())
                .buildingName(exercise.getExerciseAddr().getBuildingName())
                .femaleLevel(levelCache.femaleLevel())
                .maleLevel(levelCache.maleLevel())
                .currentParticipants(currentParticipants)
                .maxCapacity(exercise.getMaxCapacity())
                .isParticipating(participatingStatus.getOrDefault(exercise.getId(), false))
                .build();
    }

    private record PartyLevelCache(
            List<String> femaleLevel,
            List<String> maleLevel
    ) {
    }
}
