package umc.cockple.demo.domain.exercise.service.command.model;

import lombok.Builder;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

@Builder
public record ExerciseGuestInviteCommand(
        String guestName,
        Gender gender,
        Level level,
        Long inviterId
) {
}
