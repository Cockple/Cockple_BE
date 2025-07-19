package umc.cockple.demo.domain.party.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

public record PartyExerciseInfoDTO(
        Long partyId,
        Long count
) {}
