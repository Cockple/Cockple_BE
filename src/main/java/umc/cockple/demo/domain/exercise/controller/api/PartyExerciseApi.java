package umc.cockple.demo.domain.exercise.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import umc.cockple.demo.domain.exercise.dto.PartyExerciseCalendarDTO;
import umc.cockple.demo.global.response.BaseResponse;

import java.time.LocalDate;

@RequestMapping("/api/parties/{partyId}/exercises")
@ExerciseApiTag
public interface PartyExerciseApi {

    @GetMapping("/calender")
    @Operation(summary = "모임 운동 캘린더 조회",
            description = "모임 운동 캘린더를 조회합니다. 시작 날짜 ~ 종료 날짜까지의 데이터를 불러옵니다. 파라미터가 없으면 과거 1주 ~ 미래 3주까지의 데이터를 불러옵니다.")
    @ApiResponse(responseCode = "200", description = "모임 운동 캘린더 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류 또는 비즈니스 룰 위반")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 모임")
    ResponseEntity<BaseResponse<PartyExerciseCalendarDTO.Response>> getPartyExerciseCalender(
            @PathVariable Long partyId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    );
}
