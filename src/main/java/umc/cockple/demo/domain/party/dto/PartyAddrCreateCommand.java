package umc.cockple.demo.domain.party.dto;

import lombok.Builder;

@Builder
public record PartyAddrCreateCommand(
        String addr1,
        String addr2
) {
}