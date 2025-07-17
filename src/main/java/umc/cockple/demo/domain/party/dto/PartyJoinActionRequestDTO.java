package umc.cockple.demo.domain.party.dto;

import umc.cockple.demo.global.enums.RequestAction;

public record PartyJoinActionRequestDTO (
        RequestAction action
){
}