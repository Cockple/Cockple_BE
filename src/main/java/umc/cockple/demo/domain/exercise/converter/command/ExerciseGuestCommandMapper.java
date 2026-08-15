package umc.cockple.demo.domain.exercise.converter.command;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.dto.participation.ExerciseCancelDTO;
import umc.cockple.demo.domain.exercise.dto.guest.ExerciseGuestInviteDTO;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseGuestInviteCommand;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseCancelResult;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseGuestInviteResult;

@Component
public class ExerciseGuestCommandMapper {

    public ExerciseGuestInviteCommand toGuestInviteCommand(ExerciseGuestInviteDTO.Request request, Long inviterId) {
        return ExerciseGuestInviteCommand.builder()
                .guestName(request.guestName())
                .gender(request.toParsedGender())
                .level(request.toParsedLevel())
                .inviterId(inviterId)
                .build();
    }

    public ExerciseGuestInviteDTO.Response toGuestInviteResponse(ExerciseGuestInviteResult result) {
        return ExerciseGuestInviteDTO.Response.builder()
                .guestId(result.guestId())
                .invitedAt(result.invitedAt())
                .currentParticipants(result.currentParticipants())
                .build();
    }

    public ExerciseCancelDTO.Response toCancelResponse(ExerciseCancelResult result) {
        return ExerciseCancelDTO.Response.builder()
                .memberName(result.memberName())
                .currentParticipants(result.currentParticipants())
                .build();
    }
}
