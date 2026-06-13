package umc.cockple.demo.domain.exercise.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.exercise.controller.api.PartyExerciseApi;
import umc.cockple.demo.domain.exercise.dto.PartyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.service.ExerciseQueryService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Validated
public class PartyExerciseController implements PartyExerciseApi {

    private final ExerciseQueryService exerciseQueryService;

    @Override
    public ResponseEntity<BaseResponse<PartyExerciseCalendarDTO.Response>> getPartyExerciseCalender(
            Long partyId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        PartyExerciseCalendarDTO.Response response = exerciseQueryService.getPartyExerciseCalendar(
                partyId, memberId, startDate, endDate);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }
}
