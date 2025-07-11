package umc.cockple.demo.domain.party.dto;

import lombok.Builder;

@Builder
public record PartyJoinResponseDTO(
        Long joinRequestId
) {
}
