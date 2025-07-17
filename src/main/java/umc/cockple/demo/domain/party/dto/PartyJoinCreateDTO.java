package umc.cockple.demo.domain.party.dto;

import lombok.Builder;

public class PartyJoinCreateDTO {

    @Builder
    public record Response(
            Long joinRequestId
    ) {
    }
}
