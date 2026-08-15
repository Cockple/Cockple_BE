package umc.cockple.demo.domain.exercise.presentation.mapper.query;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.presentation.dto.recommendation.ExerciseRecommendationCalendarDTO;
import umc.cockple.demo.domain.exercise.presentation.dto.recommendation.ExerciseRecommendationDTO;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseRecommendationCalendarResult;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseRecommendationResult;

@Component
public class ExerciseRecommendationQueryMapper {

    public ExerciseRecommendationDTO.Response toExerciseRecommendationResponse(
            ExerciseRecommendationResult result) {
        return ExerciseRecommendationDTO.Response.builder()
                .totalExercises(result.totalExercises())
                .exercises(result.exercises().stream().map(this::toExerciseRecommendationItem).toList())
                .build();
    }

    public ExerciseRecommendationCalendarDTO.Response toRecommendationCalendarResponse(
            ExerciseRecommendationCalendarResult result) {
        return ExerciseRecommendationCalendarDTO.Response.builder()
                .startDate(result.startDate())
                .endDate(result.endDate())
                .weeks(result.weeks().stream().map(this::toRecommendationWeek).toList())
                .build();
    }

    private ExerciseRecommendationDTO.ExerciseItem toExerciseRecommendationItem(
            ExerciseRecommendationResult.ExerciseItem result) {
        return ExerciseRecommendationDTO.ExerciseItem.builder()
                .exerciseId(result.exerciseId())
                .partyId(result.partyId())
                .partyName(result.partyName())
                .date(result.date())
                .dayOfWeek(result.dayOfWeek())
                .startTime(result.startTime())
                .endTime(result.endTime())
                .buildingName(result.buildingName())
                .profileImageUrl(result.profileImageUrl())
                .isBookmarked(result.bookmarked())
                .build();
    }

    private ExerciseRecommendationCalendarDTO.WeeklyExercises toRecommendationWeek(
            ExerciseRecommendationCalendarResult.WeeklyExercises result) {
        return ExerciseRecommendationCalendarDTO.WeeklyExercises.builder()
                .weekStartDate(result.weekStartDate())
                .weekEndDate(result.weekEndDate())
                .days(result.days().stream().map(this::toRecommendationDay).toList())
                .build();
    }

    private ExerciseRecommendationCalendarDTO.DailyExercises toRecommendationDay(
            ExerciseRecommendationCalendarResult.DailyExercises result) {
        return ExerciseRecommendationCalendarDTO.DailyExercises.builder()
                .date(result.date())
                .dayOfWeek(result.dayOfWeek())
                .exercises(result.exercises().stream().map(this::toRecommendationCalendarItem).toList())
                .build();
    }

    private ExerciseRecommendationCalendarDTO.ExerciseCalendarItem toRecommendationCalendarItem(
            ExerciseRecommendationCalendarResult.ExerciseCalendarItem result) {
        return ExerciseRecommendationCalendarDTO.ExerciseCalendarItem.builder()
                .exerciseId(result.exerciseId())
                .partyId(result.partyId())
                .partyName(result.partyName())
                .buildingName(result.buildingName())
                .startTime(result.startTime())
                .endTime(result.endTime())
                .profileImageUrl(result.profileImageUrl())
                .isBookmarked(result.bookmarked())
                .distance(result.distance())
                .build();
    }
}
