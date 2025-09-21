package umc.cockple.demo.domain.party.dto;

import lombok.Builder;

public class PartySimpleDTO {
    @Builder
    public record Response(
            Long partyId,
            String partyName,
            String addr1,
            String addr2,
            String partyImgUrl
    ) {}
}
