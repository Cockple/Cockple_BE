package umc.cockple.demo.domain.exercise.presentation.mapper.query;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.presentation.dto.my.MyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.presentation.dto.my.MyExerciseListDTO;
import umc.cockple.demo.domain.exercise.presentation.dto.my.MyPartyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.presentation.dto.my.MyPartyExerciseDTO;
import umc.cockple.demo.domain.exercise.service.query.result.MyExerciseCalendarResult;
import umc.cockple.demo.domain.exercise.service.query.result.MyExerciseListResult;
import umc.cockple.demo.domain.exercise.service.query.result.MyPartyExerciseCalendarResult;
import umc.cockple.demo.domain.exercise.service.query.result.MyPartyExerciseResult;

@Component
public class ExerciseMyQueryMapper {

    public MyExerciseCalendarDTO.Response toMyExerciseCalendarResponse(MyExerciseCalendarResult result) {
        return MyExerciseCalendarDTO.Response.builder()
                .startDate(result.startDate())
                .endDate(result.endDate())
                .weeks(result.weeks().stream().map(this::toMyExerciseWeek).toList())
                .build();
    }

    public MyPartyExerciseDTO.Response toMyPartyExerciseResponse(MyPartyExerciseResult result) {
        return MyPartyExerciseDTO.Response.builder()
                .totalExercises(result.totalExercises())
                .exercises(result.exercises().stream().map(this::toMyPartyExerciseItem).toList())
                .build();
    }

    public MyPartyExerciseCalendarDTO.Response toMyPartyExerciseCalendarResponse(
            MyPartyExerciseCalendarResult result) {
        return MyPartyExerciseCalendarDTO.Response.builder()
                .startDate(result.startDate())
                .endDate(result.endDate())
                .weeks(result.weeks().stream().map(this::toMyPartyExerciseWeek).toList())
                .build();
    }

    public MyExerciseListDTO.Response toMyExerciseListResponse(MyExerciseListResult result) {
        return MyExerciseListDTO.Response.builder()
                .totalCount(result.totalCount())
                .hasNext(result.hasNext())
                .exercises(result.exercises().stream().map(this::toMyExerciseListItem).toList())
                .build();
    }

    private MyExerciseCalendarDTO.WeeklyExercises toMyExerciseWeek(
            MyExerciseCalendarResult.WeeklyExercises result) {
        return MyExerciseCalendarDTO.WeeklyExercises.builder()
                .weekStartDate(result.weekStartDate())
                .weekEndDate(result.weekEndDate())
                .days(result.days().stream().map(this::toMyExerciseDay).toList())
                .build();
    }

    private MyExerciseCalendarDTO.DailyExercises toMyExerciseDay(
            MyExerciseCalendarResult.DailyExercises result) {
        return MyExerciseCalendarDTO.DailyExercises.builder()
                .date(result.date())
                .dayOfWeek(result.date().getDayOfWeek().name())
                .exercises(result.exercises().stream().map(this::toMyExerciseCalendarItem).toList())
                .build();
    }

    private MyExerciseCalendarDTO.ExerciseCalendarItem toMyExerciseCalendarItem(
            MyExerciseCalendarResult.ExerciseCalendarItem result) {
        return MyExerciseCalendarDTO.ExerciseCalendarItem.builder()
                .exerciseId(result.exerciseId())
                .partyId(result.partyId())
                .partyName(result.partyName())
                .buildingName(result.buildingName())
                .startTime(result.startTime())
                .endTime(result.endTime())
                .profileImageUrl(result.profileImageUrl())
                .build();
    }

    private MyPartyExerciseDTO.Exercises toMyPartyExerciseItem(MyPartyExerciseResult.ExerciseItem result) {
        return MyPartyExerciseDTO.Exercises.builder()
                .exerciseId(result.exerciseId())
                .partyId(result.partyId())
                .partyName(result.partyName())
                .buildingName(result.buildingName())
                .date(result.date())
                .dayOfWeek(result.date().getDayOfWeek().name())
                .startTime(result.startTime())
                .profileImageUrl(result.profileImageUrl())
                .build();
    }

    private MyPartyExerciseCalendarDTO.WeeklyExercises toMyPartyExerciseWeek(
            MyPartyExerciseCalendarResult.WeeklyExercises result) {
        return MyPartyExerciseCalendarDTO.WeeklyExercises.builder()
                .weekStartDate(result.weekStartDate())
                .weekEndDate(result.weekEndDate())
                .days(result.days().stream().map(this::toMyPartyExerciseDay).toList())
                .build();
    }

    private MyPartyExerciseCalendarDTO.DailyExercises toMyPartyExerciseDay(
            MyPartyExerciseCalendarResult.DailyExercises result) {
        return MyPartyExerciseCalendarDTO.DailyExercises.builder()
                .date(result.date())
                .dayOfWeek(result.date().getDayOfWeek().name())
                .exercises(result.exercises().stream().map(this::toMyPartyExerciseCalendarItem).toList())
                .build();
    }

    private MyPartyExerciseCalendarDTO.ExerciseCalendarItem toMyPartyExerciseCalendarItem(
            MyPartyExerciseCalendarResult.ExerciseCalendarItem result) {
        return MyPartyExerciseCalendarDTO.ExerciseCalendarItem.builder()
                .exerciseId(result.exerciseId())
                .partyId(result.partyId())
                .partyName(result.partyName())
                .buildingName(result.buildingName())
                .startTime(result.startTime())
                .endTime(result.endTime())
                .profileImageUrl(result.profileImageUrl())
                .isBookmarked(result.bookmarked())
                .nowCapacity(result.nowCapacity())
                .build();
    }

    private MyExerciseListDTO.ExerciseItem toMyExerciseListItem(MyExerciseListResult.ExerciseItem result) {
        return MyExerciseListDTO.ExerciseItem.builder()
                .exerciseId(result.exerciseId())
                .partyId(result.partyId())
                .partyName(result.partyName())
                .isBookmarked(result.bookmarked())
                .date(result.date())
                .dayOfWeek(result.date().getDayOfWeek().name())
                .buildingName(result.buildingName())
                .startTime(result.startTime())
                .endTime(result.endTime())
                .femaleLevel(result.femaleLevel())
                .maleLevel(result.maleLevel())
                .currentParticipants(result.currentParticipants())
                .maxCapacity(result.maxCapacity())
                .isCompleted(result.completed())
                .partyGuestInviteAccept(result.partyGuestInviteAccept())
                .build();
    }
}
