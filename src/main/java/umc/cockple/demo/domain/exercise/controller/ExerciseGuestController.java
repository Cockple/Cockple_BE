package umc.cockple.demo.domain.exercise.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.exercise.controller.api.ExerciseGuestApi;
import umc.cockple.demo.domain.exercise.dto.ExerciseCancelDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseGuestInviteDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseMyGuestListDTO;
import umc.cockple.demo.domain.exercise.service.ExerciseQueryService;
import umc.cockple.demo.domain.exercise.service.command.ExerciseCommandService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

@RestController
@RequiredArgsConstructor
@Validated
public class ExerciseGuestController implements ExerciseGuestApi {

    private final ExerciseCommandService exerciseCommandService;
    private final ExerciseQueryService exerciseQueryService;

    @Override
    public ResponseEntity<BaseResponse<ExerciseGuestInviteDTO.Response>> inviteGuest(
            Long exerciseId,
            ExerciseGuestInviteDTO.Request request
    ) {
        Long inviterId = SecurityUtil.getCurrentMemberId();

        ExerciseGuestInviteDTO.Response response = exerciseCommandService.inviteGuest(
                exerciseId, inviterId, request);

        return BaseResponse.of(CommonSuccessCode.CREATED, response);
    }

    @Override
    public ResponseEntity<BaseResponse<ExerciseCancelDTO.Response>> cancelGuestInvitation(
            Long exerciseId,
            Long guestId
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseCancelDTO.Response response = exerciseCommandService.cancelGuestInvitation(
                exerciseId, guestId, memberId);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }

    @Override
    public ResponseEntity<BaseResponse<ExerciseMyGuestListDTO.Response>> getMyInvitedGuests(
            Long exerciseId
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseMyGuestListDTO.Response response = exerciseQueryService.getMyInvitedGuests(
                exerciseId, memberId);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }
}
