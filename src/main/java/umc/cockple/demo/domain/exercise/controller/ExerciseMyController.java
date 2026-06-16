package umc.cockple.demo.domain.exercise.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.exercise.controller.api.ExerciseMyApi;
import umc.cockple.demo.domain.exercise.dto.MyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.dto.MyExerciseListDTO;
import umc.cockple.demo.domain.exercise.dto.MyPartyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.dto.MyPartyExerciseDTO;
import umc.cockple.demo.domain.exercise.enums.MyExerciseFilterType;
import umc.cockple.demo.domain.exercise.enums.MyExerciseOrderType;
import umc.cockple.demo.domain.exercise.enums.MyPartyExerciseOrderType;
import umc.cockple.demo.domain.exercise.service.query.ExerciseMyQueryService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Validated
public class ExerciseMyController implements ExerciseMyApi {

    private final ExerciseMyQueryService exerciseMyQueryService;

    @Override
    public ResponseEntity<BaseResponse<MyExerciseCalendarDTO.Response>> getMyExerciseCalender(
            LocalDate startDate, LocalDate endDate) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        MyExerciseCalendarDTO.Response response = exerciseMyQueryService.getMyExerciseCalendar(
                memberId, startDate, endDate);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }

    @Override
    public ResponseEntity<BaseResponse<MyPartyExerciseDTO.Response>> getMyPartyExercise() {
        Long memberId = SecurityUtil.getCurrentMemberId();

        MyPartyExerciseDTO.Response response = exerciseMyQueryService.getMyPartyExercise(memberId);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }

    @Override
    public ResponseEntity<BaseResponse<MyPartyExerciseCalendarDTO.Response>> getMyPartyExerciseCalendar(
            MyPartyExerciseOrderType orderType, LocalDate startDate, LocalDate endDate) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        MyPartyExerciseCalendarDTO.Response response = exerciseMyQueryService.getMyPartyExerciseCalendar(
                memberId, orderType, startDate, endDate);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }

    @Override
    public ResponseEntity<BaseResponse<MyExerciseListDTO.Response>> getMyExercises(
            MyExerciseFilterType filterType, MyExerciseOrderType orderType, Pageable pageable) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        MyExerciseListDTO.Response response = exerciseMyQueryService.getMyExercises(
                memberId, filterType, orderType, pageable);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }
}
