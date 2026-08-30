package umc.cockple.demo.domain.exercise.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.exercise.presentation.controller.api.ExerciseParticipationApi;
import umc.cockple.demo.domain.exercise.presentation.mapper.command.ExerciseParticipationCommandMapper;
import umc.cockple.demo.domain.exercise.presentation.dto.participation.ExerciseCancelDTO;
import umc.cockple.demo.domain.exercise.presentation.dto.participation.ExerciseJoinDTO;
import umc.cockple.demo.domain.exercise.service.command.ExerciseParticipationCommandService;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseCancelByManagerCommand;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseCancelResult;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseJoinResult;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

@RestController
@RequiredArgsConstructor
@Validated
public class ExerciseParticipationController implements ExerciseParticipationApi {

    private final ExerciseParticipationCommandService exerciseParticipationCommandService;
    private final ExerciseParticipationCommandMapper exerciseParticipationCommandMapper;

    @Override
    public ResponseEntity<BaseResponse<ExerciseJoinDTO.Response>> joinExercise(Long exerciseId) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseJoinResult result = exerciseParticipationCommandService.joinExercise(exerciseId, memberId);
        ExerciseJoinDTO.Response response = exerciseParticipationCommandMapper.toJoinResponse(result);

        return BaseResponse.of(CommonSuccessCode.CREATED, response);
    }

    @Override
    public ResponseEntity<BaseResponse<ExerciseCancelDTO.Response>> cancelParticipation(Long exerciseId) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseCancelResult result = exerciseParticipationCommandService.cancelParticipation(exerciseId, memberId);
        ExerciseCancelDTO.Response response = exerciseParticipationCommandMapper.toCancelResponse(result);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }

    @Override
    public ResponseEntity<BaseResponse<ExerciseCancelDTO.Response>> cancelParticipationByManager(
            Long exerciseId, Long participantId, ExerciseCancelDTO.ByManagerRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseCancelByManagerCommand command = exerciseParticipationCommandMapper.toCancelByManagerCommand(request);
        ExerciseCancelResult result = exerciseParticipationCommandService.cancelParticipationByManager(
                exerciseId, participantId, memberId, command);
        ExerciseCancelDTO.Response response = exerciseParticipationCommandMapper.toCancelResponse(result);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }
}
