package umc.cockple.demo.domain.exercise.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.exercise.controller.api.ExerciseRecommendationApi;
import umc.cockple.demo.domain.exercise.dto.recommendation.ExerciseRecommendationCalendarDTO;
import umc.cockple.demo.domain.exercise.dto.recommendation.ExerciseRecommendationDTO;
import umc.cockple.demo.domain.exercise.enums.MyPartyExerciseOrderType;
import umc.cockple.demo.domain.exercise.service.query.ExerciseRecommendationQueryService;
import umc.cockple.demo.domain.party.enums.ActivityTime;
import umc.cockple.demo.domain.party.enums.ParticipationType;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
public class ExerciseRecommendationController implements ExerciseRecommendationApi {

    private final ExerciseRecommendationQueryService exerciseRecommendationQueryService;

    @Override
    public ResponseEntity<BaseResponse<ExerciseRecommendationDTO.Response>> getRecommendedExercises() {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseRecommendationDTO.Response response = exerciseRecommendationQueryService.getRecommendedExercises(memberId);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }

    @Override
    public ResponseEntity<BaseResponse<ExerciseRecommendationCalendarDTO.Response>> getRecommendedExerciseCalendar(
            LocalDate startDate,
            LocalDate endDate,
            Boolean isCockpleRecommend,
            String addr1,
            String addr2,
            List<Level> levels,
            List<ParticipationType> participationTypes,
            List<ActivityTime> activityTimes,
            MyPartyExerciseOrderType sortType
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseRecommendationCalendarDTO.FilterCondition filterCondition =
                ExerciseRecommendationCalendarDTO.FilterCondition.builder()
                        .addr1(addr1)
                        .addr2(addr2)
                        .levels(levels)
                        .participationTypes(participationTypes)
                        .activityTimes(activityTimes)
                        .build();

        ExerciseRecommendationCalendarDTO.Response response = exerciseRecommendationQueryService
                .getRecommendedExerciseCalendar(memberId, startDate, endDate, isCockpleRecommend, filterCondition, sortType);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }
}
