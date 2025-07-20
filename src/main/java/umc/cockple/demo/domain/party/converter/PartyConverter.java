package umc.cockple.demo.domain.party.converter;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyJoinRequest;
import umc.cockple.demo.domain.party.dto.*;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.ParticipationType;
import umc.cockple.demo.global.enums.RequestStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PartyConverter {

    public PartySimpleDTO.Response toPartySimpleDTO(MemberParty memberParty) {
        Party party = memberParty.getParty();
        return PartySimpleDTO.Response.builder()
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .addr1(party.getPartyAddr().getAddr1())
                .addr2(party.getPartyAddr().getAddr2())
                .partyImgUrl(party.getPartyImg() != null ? party.getPartyImg().getImgUrl() : null)
                .build();
    }

    public PartyDTO.Response toMyPartyDTO(Party party, String nextExerciseInfo, Integer totalExerciseCount) {
        //급수 조건 가공 필요
        return PartyDTO.Response.builder()
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .addr1(party.getPartyAddr().getAddr1())
                .addr2(party.getPartyAddr().getAddr2())
                .femaleLevel(getLevelList(party, Gender.FEMALE))
                .maleLevel(getLevelList(party, Gender.MALE))
                .nextExerciseInfo(nextExerciseInfo)
                .totalExerciseCount(totalExerciseCount)
                .partyImgUrl(party.getPartyImg() != null ? party.getPartyImg().getImgUrl() : null)
                .build();
    }

    public PartyDetailDTO.Response toPartyDetailResponseDTO(Party party, Optional<MemberParty> memberPartyOpt) {
        // 급수 정보 가공
        List<String> femaleLevel = getLevelList(party, Gender.FEMALE);
        List<String> maleLevel = (party.getPartyType() == ParticipationType.WOMEN_DOUBLES) ?
                null : getLevelList(party, Gender.MALE);
        // 멤버 정보 가공
        String memberStatus = memberPartyOpt.isPresent() ? "MEMBER" : "NOT_MEMBER";
        String memberRole = memberPartyOpt.map(mp -> mp.getRole().name()).orElse(null);

        return PartyDetailDTO.Response.builder()
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .memberStatus(memberStatus)
                .memberRole(memberRole)
                .addr1(party.getPartyAddr().getAddr1())
                .addr2(party.getPartyAddr().getAddr2())
                .activityDays(party.getActiveDays().stream().map(day -> day.getActiveDay().getKoreanName()).toList())
                .activityTime(party.getActivityTime().getKoreanName())
                .femaleLevel(femaleLevel)
                .maleLevel(maleLevel)
                .minAge(party.getMinAge())
                .maxAge(party.getMaxAge())
                .price(party.getPrice())
                .joinPrice(party.getJoinPrice())
                .designatedCock(party.getDesignatedCock())
                .content(party.getContent())
                .keywords(party.getKeywords().stream().map(kw -> kw.getKeyword().getKoreanName()).toList())
                .partyImgUrl(party.getPartyImg() != null ? party.getPartyImg().getImgUrl() : null)
                .build();
    }

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
        // status가 PENDING이 아닐 경우에만 updatedAt 값을 설정
        LocalDateTime updatedAt = (request.getStatus() != RequestStatus.PENDING) ? request.getUpdatedAt() : null;
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
                .updatedAt(updatedAt)
                .build();
    }

    private List<String> getLevelList(Party party, Gender gender) {
        List<String> levelList = party.getLevels().stream()
                .filter(l -> l.getGender() == gender)
                .map(l -> l.getLevel().getKoreanName())
                .toList();

        return levelList.isEmpty() ? null : levelList;
    }

}
