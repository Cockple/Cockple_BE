package umc.cockple.demo.domain.exercise.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.exercise.controller.api.ExerciseLifecycleApi;
import umc.cockple.demo.domain.exercise.converter.command.ExerciseLifecycleCommandMapper;
import umc.cockple.demo.domain.exercise.dto.lifecycle.ExerciseCreateDTO;
import umc.cockple.demo.domain.exercise.dto.lifecycle.ExerciseDeleteDTO;
import umc.cockple.demo.domain.exercise.dto.lifecycle.ExerciseDetailDTO;
import umc.cockple.demo.domain.exercise.dto.lifecycle.ExerciseEditDetailDTO;
import umc.cockple.demo.domain.exercise.dto.lifecycle.ExerciseUpdateDTO;
import umc.cockple.demo.domain.exercise.service.query.ExerciseLifecycleQueryService;
import umc.cockple.demo.domain.exercise.service.command.ExerciseLifecycleCommandService;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseCreateAddressCommand;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseCreateCommand;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseUpdateAddressCommand;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseUpdateCommand;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseCreateResult;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseDeleteResult;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseUpdateResult;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

@RestController
@RequiredArgsConstructor
@Validated
public class ExerciseLifecycleController implements ExerciseLifecycleApi {

    private final ExerciseLifecycleCommandService exerciseLifecycleCommandService;
    private final ExerciseLifecycleQueryService exerciseLifecycleQueryService;
    private final ExerciseLifecycleCommandMapper exerciseLifecycleCommandMapper;

    @Override
    public ResponseEntity<BaseResponse<ExerciseCreateDTO.Response>> createExercise(
            Long partyId, ExerciseCreateDTO.Request request) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseCreateCommand command = exerciseLifecycleCommandMapper.toCreateCommand(request);
        ExerciseCreateAddressCommand addressCommand = exerciseLifecycleCommandMapper.toAddrCreateCommand(request);
        ExerciseCreateResult result = exerciseLifecycleCommandService.createExercise(
                partyId, memberId, command, addressCommand);
        ExerciseCreateDTO.Response response = exerciseLifecycleCommandMapper.toCreateResponse(result);

        return BaseResponse.of(CommonSuccessCode.CREATED, response);
    }

    @Override
    public ResponseEntity<BaseResponse<ExerciseDeleteDTO.Response>> deleteExercise(Long exerciseId) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseDeleteResult result = exerciseLifecycleCommandService.deleteExercise(exerciseId, memberId);
        ExerciseDeleteDTO.Response response = exerciseLifecycleCommandMapper.toDeleteResponse(result);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }

    @Override
    public ResponseEntity<BaseResponse<ExerciseUpdateDTO.Response>> updateExercise(
            Long exerciseId, ExerciseUpdateDTO.Request request) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseUpdateCommand command = exerciseLifecycleCommandMapper.toUpdateCommand(request);
        ExerciseUpdateAddressCommand addressCommand = exerciseLifecycleCommandMapper.toAddrUpdateCommand(request);
        ExerciseUpdateResult result = exerciseLifecycleCommandService.updateExercise(
                exerciseId, memberId, command, addressCommand);
        ExerciseUpdateDTO.Response response = exerciseLifecycleCommandMapper.toUpdateResponse(result);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }

    @Override
    public ResponseEntity<BaseResponse<ExerciseDetailDTO.Response>> getExerciseDetail(Long exerciseId) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseDetailDTO.Response response = exerciseLifecycleQueryService.getExerciseDetail(
                exerciseId, memberId);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }

    @Override
    public ResponseEntity<BaseResponse<ExerciseEditDetailDTO.Response>> getExerciseForEdit(Long exerciseId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        ExerciseEditDetailDTO.Response response = exerciseLifecycleQueryService.getExerciseForEdit(exerciseId, memberId);
        return BaseResponse.of(CommonSuccessCode.OK, response);
    }
}
