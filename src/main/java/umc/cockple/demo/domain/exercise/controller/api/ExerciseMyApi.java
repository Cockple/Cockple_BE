package umc.cockple.demo.domain.exercise.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import umc.cockple.demo.domain.exercise.dto.MyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.dto.MyExerciseListDTO;
import umc.cockple.demo.domain.exercise.dto.MyPartyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.dto.MyPartyExerciseDTO;
import umc.cockple.demo.domain.exercise.enums.MyExerciseFilterType;
import umc.cockple.demo.domain.exercise.enums.MyExerciseOrderType;
import umc.cockple.demo.domain.exercise.enums.MyPartyExerciseOrderType;
import umc.cockple.demo.global.response.BaseResponse;

import java.time.LocalDate;

@RequestMapping("/api/exercises")
@Tag(name = "Exercise", description = "운동 관리 API")
public interface ExerciseMyApi {

    @GetMapping("/my/calender")
    @Operation(summary = "내 운동 캘린더 조회",
            description = "내 운동 캘린더를 조회합니다. 시작 날짜 ~ 종료 날짜까지의 데이터를 불러옵니다. 파라미터가 없으면 과거 1주 ~ 미래 3주까지의 데이터를 불러옵니다.")
    @ApiResponse(responseCode = "200", description = "내 운동 캘린더 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류 또는 비즈니스 룰 위반")
    ResponseEntity<BaseResponse<MyExerciseCalendarDTO.Response>> getMyExerciseCalender(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    );

    @GetMapping("/parties/my")
    @Operation(summary = "내 모임 운동 조회",
            description = "내 모임의 운동 목록을 조회합니다. 시작하지 않은 운동만 표시되며, 최대 6개의 운동만 반환합니다.")
    @ApiResponse(responseCode = "200", description = "내 모임 운동 조회 성공")
    ResponseEntity<BaseResponse<MyPartyExerciseDTO.Response>> getMyPartyExercise();

    @GetMapping("/parties/my/calendar")
    @Operation(summary = "내 모임 운동 캘린더 조회",
            description = """
                    내 모임의 운동 캘린더를 조회합니다.
                    시작 날짜 ~ 종료 날짜까지의 데이터를 불러옵니다. 파라미터가 없으면 과거 1주 ~ 미래 3주까지의 데이터를 불러옵니다.
                    정렬 방식은 최신순(LATEST)과 참여인원이 많은 순(POPULARITY) 2가지로 구분됩니다. 파라미터를 없으면 최신순으로 불러옵니다.
                    """)
    @ApiResponse(responseCode = "200", description = "내 운동 캘린더 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류 또는 비즈니스 룰 위반")
    ResponseEntity<BaseResponse<MyPartyExerciseCalendarDTO.Response>> getMyPartyExerciseCalendar(
            @RequestParam(defaultValue = "LATEST") MyPartyExerciseOrderType orderType,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    );

    @GetMapping("/my")
    @Operation(summary = "내 참여 운동 조회",
            description = """
                    내가 참여한 운동 목록을 조회합니다.
                    필터: 전체(ALL), 참여 예정(UPCOMING), 참여 완료(COMPLETED)
                    정렬: 최신순(LATEST), 오래된순(OLDEST)
                    페이징을 지원합니다.
                    """)
    @ApiResponse(responseCode = "200", description = "내 참여 운동 조회 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 필터 타입 또는 정렬 타입")
    ResponseEntity<BaseResponse<MyExerciseListDTO.Response>> getMyExercises(
            @RequestParam(defaultValue = "ALL") MyExerciseFilterType filterType,
            @RequestParam(defaultValue = "LATEST") MyExerciseOrderType orderType,
            @PageableDefault(size = 15) Pageable pageable
    );
}
