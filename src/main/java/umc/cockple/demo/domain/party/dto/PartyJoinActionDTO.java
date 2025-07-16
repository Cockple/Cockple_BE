package umc.cockple.demo.domain.party.dto;

import lombok.Builder;
import umc.cockple.demo.global.enums.RequestAction;

public class PartyJoinActionDTO {

    @Builder
    public record Request(
            RequestAction action
    ){
    }
}
