package umc.cockple.demo.domain.exercise.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.exercise.controller.api.ExerciseMyApi;
import umc.cockple.demo.domain.exercise.converter.query.ExerciseMyQueryMapper;
import umc.cockple.demo.domain.exercise.dto.my.MyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.dto.my.MyExerciseListDTO;
import umc.cockple.demo.domain.exercise.dto.my.MyPartyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.dto.my.MyPartyExerciseDTO;
import umc.cockple.demo.domain.exercise.enums.MyExerciseFilterType;
import umc.cockple.demo.domain.exercise.enums.MyExerciseOrderType;
import umc.cockple.demo.domain.exercise.enums.MyPartyExerciseOrderType;
import umc.cockple.demo.domain.exercise.service.query.ExerciseMyQueryService;
import umc.cockple.demo.domain.exercise.service.query.result.MyExerciseCalendarResult;
import umc.cockple.demo.domain.exercise.service.query.result.MyExerciseListResult;
import umc.cockple.demo.domain.exercise.service.query.result.MyPartyExerciseCalendarResult;
import umc.cockple.demo.domain.exercise.service.query.result.MyPartyExerciseResult;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Validated
public class ExerciseMyController implements ExerciseMyApi {

    private final ExerciseMyQueryService exerciseMyQueryService;
    private final ExerciseMyQueryMapper exerciseMyQueryMapper;

    @Override
    public ResponseEntity<BaseResponse<MyExerciseCalendarDTO.Response>> getMyExerciseCalender(
            LocalDate startDate, LocalDate endDate) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        MyExerciseCalendarResult result = exerciseMyQueryService.getMyExerciseCalendar(
                memberId, startDate, endDate);
        MyExerciseCalendarDTO.Response response = exerciseMyQueryMapper.toMyExerciseCalendarResponse(result);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }

    @Override
    public ResponseEntity<BaseResponse<MyPartyExerciseDTO.Response>> getMyPartyExercise() {
        Long memberId = SecurityUtil.getCurrentMemberId();

        MyPartyExerciseResult result = exerciseMyQueryService.getMyPartyExercise(memberId);
        MyPartyExerciseDTO.Response response = exerciseMyQueryMapper.toMyPartyExerciseResponse(result);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }

    @Override
    public ResponseEntity<BaseResponse<MyPartyExerciseCalendarDTO.Response>> getMyPartyExerciseCalendar(
            MyPartyExerciseOrderType orderType, LocalDate startDate, LocalDate endDate) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        MyPartyExerciseCalendarResult result = exerciseMyQueryService.getMyPartyExerciseCalendar(
                memberId, orderType, startDate, endDate);
        MyPartyExerciseCalendarDTO.Response response =
                exerciseMyQueryMapper.toMyPartyExerciseCalendarResponse(result);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }

    @Override
    public ResponseEntity<BaseResponse<MyExerciseListDTO.Response>> getMyExercises(
            MyExerciseFilterType filterType, MyExerciseOrderType orderType, Pageable pageable) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        MyExerciseListResult result = exerciseMyQueryService.getMyExercises(
                memberId, filterType, orderType, pageable);
        MyExerciseListDTO.Response response = exerciseMyQueryMapper.toMyExerciseListResponse(result);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }
}
