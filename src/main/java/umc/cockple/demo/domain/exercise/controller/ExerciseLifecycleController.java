package umc.cockple.demo.domain.exercise.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.exercise.controller.api.ExerciseLifecycleApi;
import umc.cockple.demo.domain.exercise.dto.ExerciseCreateDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseDeleteDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseDetailDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseEditDetailDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseUpdateDTO;
import umc.cockple.demo.domain.exercise.service.ExerciseQueryService;
import umc.cockple.demo.domain.exercise.service.command.ExerciseCommandService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

@RestController
@RequiredArgsConstructor
@Validated
public class ExerciseLifecycleController implements ExerciseLifecycleApi {

    private final ExerciseCommandService exerciseCommandService;
    private final ExerciseQueryService exerciseQueryService;

    @Override
    public ResponseEntity<BaseResponse<ExerciseCreateDTO.Response>> createExercise(
            Long partyId,
            ExerciseCreateDTO.Request request
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseCreateDTO.Response response = exerciseCommandService.createExercise(
                partyId, memberId, request);

        return BaseResponse.of(CommonSuccessCode.CREATED, response);
    }

    @Override
    public ResponseEntity<BaseResponse<ExerciseDeleteDTO.Response>> deleteExercise(
            Long exerciseId
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseDeleteDTO.Response response = exerciseCommandService.deleteExercise(
                exerciseId, memberId);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }

    @Override
    public ResponseEntity<BaseResponse<ExerciseUpdateDTO.Response>> updateExercise(
            Long exerciseId,
            ExerciseUpdateDTO.Request request
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseUpdateDTO.Response response = exerciseCommandService.updateExercise(
                exerciseId, memberId, request);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }

    @Override
    public ResponseEntity<BaseResponse<ExerciseDetailDTO.Response>> getExerciseDetail(
            Long exerciseId
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseDetailDTO.Response response = exerciseQueryService.getExerciseDetail(
                exerciseId, memberId);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }

    @Override
    public ResponseEntity<BaseResponse<ExerciseEditDetailDTO.Response>> getExerciseForEdit(
            Long exerciseId
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        ExerciseEditDetailDTO.Response response = exerciseQueryService.getExerciseForEdit(exerciseId, memberId);
        return BaseResponse.of(CommonSuccessCode.OK, response);
    }
}
