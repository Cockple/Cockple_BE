package umc.cockple.demo.domain.exercise.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import umc.cockple.demo.domain.exercise.dto.ExerciseRecommendationCalendarDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseRecommendationDTO;
import umc.cockple.demo.domain.exercise.enums.MyPartyExerciseOrderType;
import umc.cockple.demo.domain.party.enums.ActivityTime;
import umc.cockple.demo.domain.party.enums.ParticipationType;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.response.BaseResponse;

import java.time.LocalDate;
import java.util.List;

@RequestMapping("/api/exercises/recommendations")
@ExerciseApiTag
public interface ExerciseRecommendationApi {

    @GetMapping
    @Operation(summary = "사용자 추천 운동 조회",
            description = """
                    사용자가 속하지 않은 모임의 운동을 추천합니다.
                    조회되는 운동의 최대 개수는 10개입니다.
                    시작하지 않은 운동만 조회됩니다.
                    참여하지 않은 운동만 조회됩니다.
                    운동의 급수와 나이 조건이 사용자와 맞는 운동만 조회됩니다.
                    정렬 기준은 위치, 날짜, 시간 순입니다.
                    """)
    @ApiResponse(responseCode = "200", description = "내 운동 캘린더 성공")
    ResponseEntity<BaseResponse<ExerciseRecommendationDTO.Response>> getRecommendedExercises();

    @GetMapping("/calendar")
    @Operation(summary = "사용자 추천 운동 캘린더 조회",
            description = """
                    사용자가 속하지 않은 모임의 운동 중 참여하지 않은 운동을 캘린더 형식으로 조회합니다.
                    - isCockpleRecommend=true: 콕플 추천 (급수 일치, 위치+시간순 정렬)
                    - isCockpleRecommend=false: 필터 + 정렬 방식
                    기본 기간: 과거 1주 ~ 미래 3주
                    """)
    @ApiResponse(responseCode = "200", description = "사용자 추천 운동 캘린더 조회 성공")
    ResponseEntity<BaseResponse<ExerciseRecommendationCalendarDTO.Response>> getRecommendedExerciseCalendar(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "true") Boolean isCockpleRecommend,
            @RequestParam(required = false) String addr1,
            @RequestParam(required = false) String addr2,
            @RequestParam(required = false) List<Level> levels,
            @RequestParam(required = false) List<ParticipationType> participationTypes,
            @RequestParam(required = false) List<ActivityTime> activityTimes,
            @RequestParam(defaultValue = "LATEST") MyPartyExerciseOrderType sortType
    );
}
