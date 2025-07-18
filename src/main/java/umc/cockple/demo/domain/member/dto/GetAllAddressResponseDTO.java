package umc.cockple.demo.domain.member.dto;

import lombok.Builder;

@Builder
public record GetAllAddressResponseDTO(
        Long addrId,
        String addr1,
        String addr2,
        String addr3,
        String streetAddr,
        String buildingName,
        Float latitude,
        Float longitude,
        Boolean isMainAddr
) {
}
