package umc.cockple.demo.domain.party.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import umc.cockple.demo.domain.party.enums.RequestAction;

public class PartyInviteActionDTO {

    @Builder
    @Schema(name = "PartyInviteRequestDTO", description = "모임 초대 처리 요청")
    public record Request(
            RequestAction action
    ){}
}
