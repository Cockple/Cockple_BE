package umc.cockple.demo.domain.exercise.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.exercise.presentation.controller.api.ExerciseGuestApi;
import umc.cockple.demo.domain.exercise.presentation.mapper.command.ExerciseGuestCommandMapper;
import umc.cockple.demo.domain.exercise.presentation.mapper.query.ExerciseGuestQueryMapper;
import umc.cockple.demo.domain.exercise.presentation.dto.participation.ExerciseCancelDTO;
import umc.cockple.demo.domain.exercise.presentation.dto.guest.ExerciseGuestInviteDTO;
import umc.cockple.demo.domain.exercise.presentation.dto.guest.ExerciseMyGuestListDTO;
import umc.cockple.demo.domain.exercise.service.query.ExerciseGuestQueryService;
import umc.cockple.demo.domain.exercise.service.command.ExerciseGuestCommandService;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseGuestInviteCommand;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseCancelResult;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseGuestInviteResult;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseMyGuestListResult;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

@RestController
@RequiredArgsConstructor
@Validated
public class ExerciseGuestController implements ExerciseGuestApi {

    private final ExerciseGuestCommandService exerciseGuestCommandService;
    private final ExerciseGuestQueryService exerciseGuestQueryService;
    private final ExerciseGuestCommandMapper exerciseGuestCommandMapper;
    private final ExerciseGuestQueryMapper exerciseGuestQueryMapper;

    @Override
    public ResponseEntity<BaseResponse<ExerciseGuestInviteDTO.Response>> inviteGuest(
            Long exerciseId, ExerciseGuestInviteDTO.Request request) {
        Long inviterId = SecurityUtil.getCurrentMemberId();

        ExerciseGuestInviteCommand command = exerciseGuestCommandMapper.toGuestInviteCommand(request, inviterId);
        ExerciseGuestInviteResult result = exerciseGuestCommandService.inviteGuest(exerciseId, command);
        ExerciseGuestInviteDTO.Response response = exerciseGuestCommandMapper.toGuestInviteResponse(result);

        return BaseResponse.of(CommonSuccessCode.CREATED, response);
    }

    @Override
    public ResponseEntity<BaseResponse<ExerciseCancelDTO.Response>> cancelGuestInvitation(
            Long exerciseId, Long guestId) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseCancelResult result = exerciseGuestCommandService.cancelGuestInvitation(
                exerciseId, guestId, memberId);
        ExerciseCancelDTO.Response response = exerciseGuestCommandMapper.toCancelResponse(result);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }

    @Override
    public ResponseEntity<BaseResponse<ExerciseMyGuestListDTO.Response>> getMyInvitedGuests(Long exerciseId) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseMyGuestListResult result = exerciseGuestQueryService.getMyInvitedGuests(exerciseId, memberId);
        ExerciseMyGuestListDTO.Response response = exerciseGuestQueryMapper.toGuestListResponse(result);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }
}
