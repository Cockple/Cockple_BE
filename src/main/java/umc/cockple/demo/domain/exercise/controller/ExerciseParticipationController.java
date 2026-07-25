package umc.cockple.demo.domain.exercise.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.exercise.controller.api.ExerciseParticipationApi;
import umc.cockple.demo.domain.exercise.dto.participation.ExerciseCancelDTO;
import umc.cockple.demo.domain.exercise.dto.participation.ExerciseJoinDTO;
import umc.cockple.demo.domain.exercise.service.command.ExerciseParticipationCommandService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

@RestController
@RequiredArgsConstructor
@Validated
public class ExerciseParticipationController implements ExerciseParticipationApi {

    private final ExerciseParticipationCommandService exerciseParticipationCommandService;

    @Override
    public ResponseEntity<BaseResponse<ExerciseJoinDTO.Response>> joinExercise(Long exerciseId) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseJoinDTO.Response response = exerciseParticipationCommandService.joinExercise(
                exerciseId, memberId);

        return BaseResponse.of(CommonSuccessCode.CREATED, response);
    }

    @Override
    public ResponseEntity<BaseResponse<ExerciseCancelDTO.Response>> cancelParticipation(Long exerciseId) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseCancelDTO.Response response = exerciseParticipationCommandService.cancelParticipation(
                exerciseId, memberId);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }

    @Override
    public ResponseEntity<BaseResponse<ExerciseCancelDTO.Response>> cancelParticipationByManager(
            Long exerciseId, Long participantId, ExerciseCancelDTO.ByManagerRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseCancelDTO.Response response = exerciseParticipationCommandService.cancelParticipationByManager(
                exerciseId, participantId, memberId, request);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }
}
