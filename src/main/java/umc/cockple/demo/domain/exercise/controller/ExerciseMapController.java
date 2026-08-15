package umc.cockple.demo.domain.exercise.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.exercise.controller.api.ExerciseMapApi;
import umc.cockple.demo.domain.exercise.converter.query.ExerciseMapQueryMapper;
import umc.cockple.demo.domain.exercise.dto.map.ExerciseBuildingDetailDTO;
import umc.cockple.demo.domain.exercise.dto.map.ExerciseMapBuildingsDTO;
import umc.cockple.demo.domain.exercise.service.query.ExerciseMapQueryService;
import umc.cockple.demo.domain.exercise.service.query.model.ExerciseMapSearchQuery;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseBuildingDetailResult;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseMapBuildingsResult;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Validated
public class ExerciseMapController implements ExerciseMapApi {

    private final ExerciseMapQueryService exerciseMapQueryService;
    private final ExerciseMapQueryMapper exerciseMapQueryMapper;

    @Override
    public ResponseEntity<BaseResponse<ExerciseBuildingDetailDTO.Response>> getBuildingExerciseDetails(
            LocalDate date, String buildingName, String streetAddr) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseBuildingDetailResult result = exerciseMapQueryService
                .getBuildingExerciseDetails(buildingName, streetAddr, date, memberId);
        ExerciseBuildingDetailDTO.Response response = exerciseMapQueryMapper.toBuildingDetailResponse(result);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }

    @Override
    public ResponseEntity<BaseResponse<ExerciseMapBuildingsDTO.Response>> getMonthlyExerciseBuildings(
            LocalDate date, Double latitude, Double longitude, Double radiusKm) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        ExerciseMapSearchQuery query = ExerciseMapSearchQuery.of(date, latitude, longitude, radiusKm);

        ExerciseMapBuildingsResult result = exerciseMapQueryService
                .getExerciseMapCalendarSummary(query, memberId);
        ExerciseMapBuildingsDTO.Response response = exerciseMapQueryMapper.toMapCalendarSummaryResponse(result);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }
}
