package umc.cockple.demo.domain.exercise.converter.command;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.participation.ExerciseCancelDTO;
import umc.cockple.demo.domain.exercise.dto.guest.ExerciseGuestInviteDTO;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseGuestInviteCommand;

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

    public ExerciseGuestInviteDTO.Response toGuestInviteResponse(Guest guest, Exercise exercise) {
        return ExerciseGuestInviteDTO.Response.builder()
                .guestId(guest.getId())
                .invitedAt(guest.getCreatedAt())
                .currentParticipants(exercise.getNowCapacity())
                .build();
    }

    public ExerciseCancelDTO.Response toCancelResponse(Exercise exercise, Guest guest) {
        return ExerciseCancelDTO.Response.builder()
                .memberName(guest.getGuestName())
                .currentParticipants(exercise.getNowCapacity())
                .build();
    }
}
