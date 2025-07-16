package umc.cockple.demo.domain.member.dto;

import jakarta.validation.constraints.NotBlank;

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
            @NotBlank(message = "위도는 필수입니다.")
            Float latitude,
            @NotBlank(message = "경도는 필수입니다.")
            Float longitude,
            @NotBlank(message = "이전 대표주소id는 필수입니다.")
            Long prevMainAddrId,
            @NotBlank(message = "대표주소id는 필수입니다.")
            Long nowMainAddrId
    ) {
    }

    public record CreateMemberAddrResponseDTO(
            Long memberAddrId
    ) {
    }
}
