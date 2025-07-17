package umc.cockple.demo.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateMemberAddrDTO {

    public record CreateMemberAddrRequestDTO(

            @NotBlank(message = "주소는 필수입니다.")
            String addr1,
            @NotBlank(message = "주소는 필수입니다.")
            String addr2,
            @NotBlank(message = "주소는 필수입니다.")
            String addr3,
            @NotBlank(message = "도로명은 필수입니다.")
            String streetAddr,
            @NotBlank(message = "건물명은 필수입니다.")
            String buildingName,
            @NotNull(message = "위도는 필수입니다.")
            Float latitude,
            @NotNull(message = "경도는 필수입니다.")
            Float longitude

    ) {
    }

    public record CreateMemberAddrResponseDTO(
            Long memberAddrId
    ) {
    }
}
