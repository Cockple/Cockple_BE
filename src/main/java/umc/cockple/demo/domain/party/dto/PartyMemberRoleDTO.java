package umc.cockple.demo.domain.party.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import umc.cockple.demo.global.enums.Role;

public class PartyMemberRoleDTO {

    @Schema(name = "PartyMemberRoleRequest")
    public record Request(
            @NotNull(message = "역할 값은 필수입니다.") Role role) {
    }
}
