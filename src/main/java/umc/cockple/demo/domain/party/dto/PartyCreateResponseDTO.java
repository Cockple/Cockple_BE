package umc.cockple.demo.domain.party.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PartyCreateResponseDTO(
        Long partyId,
        LocalDateTime createdAt
) {
}