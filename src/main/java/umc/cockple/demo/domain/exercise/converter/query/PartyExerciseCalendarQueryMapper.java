package umc.cockple.demo.domain.exercise.converter.query;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.dto.party.PartyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.service.query.result.PartyExerciseCalendarResult;

@Component
public class PartyExerciseCalendarQueryMapper {

    public PartyExerciseCalendarDTO.Response toPartyExerciseCalendarResponse(
            PartyExerciseCalendarResult result) {
        return PartyExerciseCalendarDTO.Response.builder()
                .startDate(result.startDate())
                .endDate(result.endDate())
                .isMember(result.member())
                .partyName(result.partyName())
                .weeks(result.weeks().stream().map(this::toWeek).toList())
                .build();
    }

    private PartyExerciseCalendarDTO.WeeklyExercises toWeek(
            PartyExerciseCalendarResult.WeeklyExercises result) {
        return PartyExerciseCalendarDTO.WeeklyExercises.builder()
                .weekStartDate(result.weekStartDate())
                .weekEndDate(result.weekEndDate())
                .days(result.days().stream().map(this::toDay).toList())
                .build();
    }

    private PartyExerciseCalendarDTO.DailyExercises toDay(
            PartyExerciseCalendarResult.DailyExercises result) {
        return PartyExerciseCalendarDTO.DailyExercises.builder()
                .date(result.date())
                .dayOfWeek(result.dayOfWeek())
                .exercises(result.exercises().stream().map(this::toExerciseItem).toList())
                .build();
    }

    private PartyExerciseCalendarDTO.ExerciseCalendarItem toExerciseItem(
            PartyExerciseCalendarResult.ExerciseCalendarItem result) {
        return PartyExerciseCalendarDTO.ExerciseCalendarItem.builder()
                .exerciseId(result.exerciseId())
                .isBookmarked(result.bookmarked())
                .startTime(result.startTime())
                .endTime(result.endTime())
                .buildingName(result.buildingName())
                .femaleLevel(result.femaleLevel())
                .maleLevel(result.maleLevel())
                .currentParticipants(result.currentParticipants())
                .maxCapacity(result.maxCapacity())
                .isParticipating(result.participating())
                .build();
    }
}
