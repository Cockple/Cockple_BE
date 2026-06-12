package umc.cockple.demo.domain.exercise.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.exercise.dto.ExerciseBuildingDetailDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseMapBuildingsDTO;
import umc.cockple.demo.domain.exercise.service.ExerciseQueryService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@Tag(name = "Exercise", description = "운동 관리 API")
public class ExerciseController {

    private final ExerciseQueryService exerciseQueryService;

    @GetMapping("/buildings/exercises/{date}")
    @Operation(summary = "건물 운동 상세 조회",
            description = "특정 날짜 및 건물의 운동 상세 정보를 조회합니다.")
    public BaseResponse<ExerciseBuildingDetailDTO.Response> getBuildingExerciseDetails(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam String buildingName,
            @RequestParam String streetAddr
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseBuildingDetailDTO.Response response = exerciseQueryService
                .getBuildingExerciseDetails(buildingName, streetAddr, date, memberId);

        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping("/buildings/map/monthly")
    @Operation(summary = "월간 운동 건물 지도 데이터 조회",
            description = "특정 날짜가 속한 월에 운동이 개최되는 반경 내 건물들의 위치 정보를 지도 표시용으로 반환")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public BaseResponse<ExerciseMapBuildingsDTO.Response> getMonthlyExerciseBuildings(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(defaultValue = "3.0") Double radiusKm
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        ExerciseMapBuildingsDTO.Query query = ExerciseMapBuildingsDTO.Query.of(date, latitude, longitude, radiusKm);

        ExerciseMapBuildingsDTO.Response response = exerciseQueryService
                .getExerciseMapCalendarSummary(query, memberId);

        return BaseResponse.success(CommonSuccessCode.OK, response);
    }
}
