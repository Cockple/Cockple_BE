package umc.cockple.demo.domain.party.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import umc.cockple.demo.domain.party.enums.RequestAction;

public class PartyJoinActionDTO {

    @Builder
    @Schema(name = "PartyJoinActionRequestDTO", description = "모임 가입 신청 처리 요청")
    public record Request(
            RequestAction action
    ){
    }
}