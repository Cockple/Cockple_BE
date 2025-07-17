package umc.cockple.demo.domain.party.converter;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyJoinRequest;
import umc.cockple.demo.domain.party.dto.PartyCreateDTO;
import umc.cockple.demo.domain.party.dto.PartyJoinCreateDTO;
import umc.cockple.demo.domain.party.dto.PartyJoinDTO;

@Component
public class PartyConverter {

    //모임 생성 요청 DTO를 PartyCreateDTO.Command로 변환
    public PartyCreateDTO.Command toCreateCommand(PartyCreateDTO.Request request){
        return PartyCreateDTO.Command.builder()
                .partyName(request.partyName())
                .partyType(request.toParticipationTypeEnum())
                .femaleLevel(request.toFemaleLevelEnumList())
                .maleLevel(request.toMaleLevelEnumList())
                .activityDay(request.toActiveDayEnumList())
                .activityTime(request.toActivityTimeEnum())
                .minAge(request.minAge())
                .maxAge(request.maxAge())
                .price(request.price())
                .joinPrice(request.joinPrice())
                .designatedCock(request.designatedCock())
                .content(request.content())
                .imgUrl(request.imgUrl())
                .build();
    }

    //모임 생성 요청 DTO를 PartyCreateDTO.AddrCommand로 변환
    public PartyCreateDTO.AddrCommand toAddrCreateCommand(PartyCreateDTO.Request request) {
        return PartyCreateDTO.AddrCommand.builder()
                .addr1(request.addr1())
                .addr2(request.addr2())
                .build();
    }

    //모임 가입신청을 응답 DTO로 변환
    public PartyJoinCreateDTO.Response toJoinResponseDTO(PartyJoinRequest request) {
        return PartyJoinCreateDTO.Response.builder()
                .joinRequestId(request.getId())
                .build();
    }

    //모임 엔티티를 응답 DTO로 변환
    public PartyCreateDTO.Response toCreateResponseDTO(Party party) {
        return PartyCreateDTO.Response.builder()
                .partyId(party.getId())
                .createdAt(party.getCreatedAt())
                .build();
    }

    //모임 가입신청의 정보를 응답 DTO로 변환
    public PartyJoinDTO.Response toPartyJoinResponseDTO(PartyJoinRequest request) {
        Member member = request.getMember();
        //이미지가 null인 경우 null을 전달
        String imageUrl = (member.getProfileImg() != null) ? member.getProfileImg().getImgUrl() : null;
        return PartyJoinDTO.Response.builder()
                .joinRequestId(request.getId())
                .userId(member.getId())
                .nickname(member.getNickname())
                .profileImageUrl(imageUrl)
                .gender(member.getGender().name())
                .level(member.getLevel().name())
                .createdAt(request.getCreatedAt())
                .build();
    }
}
