package umc.cockple.demo.domain.exercise.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.exercise.controller.api.ExerciseMapApi;
import umc.cockple.demo.domain.exercise.dto.ExerciseBuildingDetailDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseMapBuildingsDTO;
import umc.cockple.demo.domain.exercise.service.ExerciseQueryService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Validated
public class ExerciseMapController implements ExerciseMapApi {

    private final ExerciseQueryService exerciseQueryService;

    @Override
    public ResponseEntity<BaseResponse<ExerciseBuildingDetailDTO.Response>> getBuildingExerciseDetails(
            LocalDate date,
            String buildingName,
            String streetAddr
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseBuildingDetailDTO.Response response = exerciseQueryService
                .getBuildingExerciseDetails(buildingName, streetAddr, date, memberId);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }

    @Override
    public ResponseEntity<BaseResponse<ExerciseMapBuildingsDTO.Response>> getMonthlyExerciseBuildings(
            LocalDate date,
            Double latitude,
            Double longitude,
            Double radiusKm
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        ExerciseMapBuildingsDTO.Query query = ExerciseMapBuildingsDTO.Query.of(date, latitude, longitude, radiusKm);

        ExerciseMapBuildingsDTO.Response response = exerciseQueryService
                .getExerciseMapCalendarSummary(query, memberId);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }
}
