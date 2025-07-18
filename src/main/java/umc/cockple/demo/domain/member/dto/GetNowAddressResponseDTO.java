package umc.cockple.demo.domain.member.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record GetNowAddressResponseDTO(
        Long memberAddrId,
        String addr3,
        String buildingName,
        String streetAddr
) {
}
