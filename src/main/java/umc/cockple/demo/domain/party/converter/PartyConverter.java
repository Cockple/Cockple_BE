package umc.cockple.demo.domain.party.converter;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.dto.PartyAddrCreateCommand;
import umc.cockple.demo.domain.party.dto.PartyCreateCommand;
import umc.cockple.demo.domain.party.dto.PartyCreateRequestDTO;
import umc.cockple.demo.domain.party.dto.PartyCreateResponseDTO;

@Component
public class PartyConverter {

    //요청 DTO를 PartyCreateCommand로 변환
    public PartyCreateCommand toCreateCommand(PartyCreateRequestDTO request){
        return PartyCreateCommand.builder()
                .partyName(request.partyName())
                .partyType(request.partyType())
                .femaleLevel(request.femaleLevel())
                .maleLevel(request.maleLevel())
                .activityDay(request.activityDay())
                .activityTime(request.activityTime())
                .minAge(request.minAge())
                .maxAge(request.maxAge())
                .price(request.price())
                .joinFee(request.joinFee())
                .designatedCock(request.designatedCock())
                .content(request.content())
                .build();
    }

    //요청 DTO를 PartyAddrCreateCommand로 변환
    public PartyAddrCreateCommand toAddrCreateCommand(PartyCreateRequestDTO request) {
        return PartyAddrCreateCommand.builder()
                .addr1(request.addr1())
                .addr2(request.addr2())
                .build();
    }

    //엔티티를 응답으로 변환
    public PartyCreateResponseDTO toCreateResponseDTO(Party party) {
        return PartyCreateResponseDTO.builder()
                .partyId(party.getId())
                .createdAt(party.getCreatedAt())
                .build();
    }
}
