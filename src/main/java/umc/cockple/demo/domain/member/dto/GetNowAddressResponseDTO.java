package umc.cockple.demo.domain.member.dto;

import lombok.Builder;

@Builder
public record GetNowAddressResponseDTO(
        Long memberAddrId,
        String addr3,
        Float latitude,
        Float longitude
) {
}
