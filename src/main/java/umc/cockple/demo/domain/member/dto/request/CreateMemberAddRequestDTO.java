package umc.cockple.demo.domain.member.dto.request;

public record CreateMemberAddRequestDTO(
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
