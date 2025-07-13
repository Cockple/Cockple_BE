package umc.cockple.demo.domain.exercise.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record GuestInviteResponseDTO(
        Long guestId,
        Integer participantNumber,
        LocalDateTime invitedAt,
        Integer currentParticipants
) {
}
