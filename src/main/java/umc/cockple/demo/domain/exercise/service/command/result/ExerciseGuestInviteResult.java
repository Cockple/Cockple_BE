package umc.cockple.demo.domain.exercise.service.command.result;

import java.time.LocalDateTime;

public record ExerciseGuestInviteResult(
        Long guestId,
        LocalDateTime invitedAt,
        Integer currentParticipants
) {
}
