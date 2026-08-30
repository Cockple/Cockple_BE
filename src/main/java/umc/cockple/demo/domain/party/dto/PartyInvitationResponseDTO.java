package umc.cockple.demo.domain.party.dto;

import lombok.Builder;
import umc.cockple.demo.domain.party.enums.RequestStatus;

import java.time.LocalDateTime;

@Builder
public record PartyInvitationResponseDTO(
        Long invitationId,
        Long partyId,
        String partyName,
        Long inviterId,
        String inviterNickname,
        RequestStatus status,
        LocalDateTime createdAt
) {
}
