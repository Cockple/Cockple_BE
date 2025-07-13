package umc.cockple.demo.domain.exercise.dto;

import java.time.LocalDateTime;

public record GuestInviteResponseDTO(
        Long guestId,
        Integer participantNumber,
        LocalDateTime invitedAt,
        Integer currentParticipants
) {
}
