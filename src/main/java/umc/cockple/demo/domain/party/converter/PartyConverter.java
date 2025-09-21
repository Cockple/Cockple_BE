package umc.cockple.demo.domain.party.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.image.service.ImageService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyInvitation;
import umc.cockple.demo.domain.party.domain.PartyJoinRequest;
import umc.cockple.demo.domain.party.dto.*;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.domain.party.enums.ParticipationType;
import umc.cockple.demo.domain.party.enums.RequestStatus;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PartyConverter {

    public PartySimpleDTO.Response toPartySimpleDTO(MemberParty memberParty, String imgUrl) {
        Party party = memberParty.getParty();
        return PartySimpleDTO.Response.builder()
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .addr1(party.getPartyAddr().getAddr1())
                .addr2(party.getPartyAddr().getAddr2())
                .partyImgUrl(party.getPartyImg() != null ? imgUrl : null)
                .build();
    }

    public PartyDTO.Response toMyPartyDTO(Party party, String nextExerciseInfo, Integer totalExerciseCount, String imgUrl, Boolean isBookmarked) {
        //급수 조건 가공 필요
        return PartyDTO.Response.builder()
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .isBookmarked(isBookmarked)
                .addr1(party.getPartyAddr().getAddr1())
                .addr2(party.getPartyAddr().getAddr2())
                .femaleLevel(getLevelList(party, Gender.FEMALE))
                .maleLevel(getLevelList(party, Gender.MALE))
                .nextExerciseInfo(nextExerciseInfo)
                .totalExerciseCount(totalExerciseCount)
                .partyImgUrl(party.getPartyImg() != null ? imgUrl : null)
                .build();
    }

    public PartyDetailDTO.Response toPartyDetailResponseDTO(Party party, Optional<MemberParty> memberPartyOpt, String imgUrl, boolean hasPendingJoinRequest, boolean isBookmarked) {
        //급수 정보 가공
        List<String> femaleLevel = getLevelList(party, Gender.FEMALE);
        List<String> maleLevel = (party.getPartyType() == ParticipationType.WOMEN_DOUBLES) ?
                null : getLevelList(party, Gender.MALE);
        //멤버 정보 가공
        String memberStatus = memberPartyOpt.isPresent() ? "MEMBER" : "NOT_MEMBER";
        String memberRole = memberPartyOpt.map(mp -> mp.getRole().name()).orElse(null);
        Boolean pendingRequestStatus = "NOT_MEMBER".equals(memberStatus) ? hasPendingJoinRequest : null;

        return PartyDetailDTO.Response.builder()
                .partyId(party.getId())
                .ownerId(party.getOwnerId())
                .partyName(party.getPartyName())
                .memberStatus(memberStatus)
                .memberRole(memberRole)
                .hasPendingJoinRequest(pendingRequestStatus)
                .isBookmarked(isBookmarked)
                .addr1(party.getPartyAddr().getAddr1())
                .addr2(party.getPartyAddr().getAddr2())
                .activityDays(party.getActiveDays().stream().map(day -> day.getActiveDay().getKoreanName()).toList())
                .activityTime(party.getActivityTime().getKoreanName())
                .femaleLevel(femaleLevel)
                .maleLevel(maleLevel)
                .minBirthYear(party.getMinBirthYear())
                .maxBirthYear(party.getMaxBirthYear())
                .price(party.getPrice())
                .joinPrice(party.getJoinPrice())
                .designatedCock(party.getDesignatedCock())
                .content(party.getContent())
                .keywords(party.getKeywords().stream().map(kw -> kw.getKeyword().getKoreanName()).toList())
                .partyImgUrl(party.getPartyImg() != null ? imgUrl : null)
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
                .minBirthYear(request.minBirthYear())
                .maxBirthYear(request.maxBirthYear())
                .price(request.price())
                .joinPrice(request.joinPrice())
                .designatedCock(request.designatedCock())
                .content(request.content())
                .imgKey(request.imgKey())
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
    public PartyJoinCreateDTO.Response toJoinResponseDTO(PartyJoinRequest joinRequest) {
        return PartyJoinCreateDTO.Response.builder()
                .joinRequestId(joinRequest.getId())
                .build();
    }

    //모임 멤버 추천을 응답 DTO로 변환
    public PartyInviteCreateDTO.Response toInviteResponseDTO(PartyInvitation invitation){
        return PartyInviteCreateDTO.Response.builder()
                .invitationId(invitation.getId())
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
        String imageUrl = (member.getProfileImg() != null) ? member.getProfileImg().getImgKey() : null;
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

    public PartyMemberDTO.Response toPartyMemberDTO(List<MemberParty> memberParties, Long currentMemberId) {
        //멤버 리스트 생성
        List<PartyMemberDTO.MemberDetail> memberDetails = memberParties.stream()
                .map(mp -> {
                    Member member = mp.getMember();
                    return PartyMemberDTO.MemberDetail.builder()
                            .memberId(member.getId())
                            .nickname(member.getNickname())
                            .profileImageUrl(member.getProfileImg() != null ? member.getProfileImg().getImgKey() : null)
                            .role(mp.getRole().name())
                            .gender(member.getGender().name())
                            .level(member.getLevel().getKoreanName())
                            .isMe(member.getId().equals(currentMemberId))
                            .build();
                })
                //Role에 따라 정렬 (모임장, 부모임장이 위로 가도록 정렬)
                .sorted(Comparator.comparing(detail -> getRolePriority(detail.role()))) // .sort() 대신 .sorted() 사용
                .toList();

        //모임의 멤버 관련 정보 리스트 생성
        long femaleCount = memberDetails.stream().filter(md -> "FEMALE".equals(md.gender())).count();
        long maleCount = memberDetails.stream().filter(md -> "MALE".equals(md.gender())).count();
        PartyMemberDTO.Summary summary = PartyMemberDTO.Summary.builder()
                .totalCount(memberDetails.size())
                .femaleCount((int) femaleCount)
                .maleCount((int) maleCount)
                .build();

        return new PartyMemberDTO.Response(summary, memberDetails);
    }

    //추천 멤버 DTO로 변환
    public PartyMemberSuggestionDTO.Response toPartyMemberSuggestionDTO(Member member, String imgUrl) {
        return PartyMemberSuggestionDTO.Response.builder()
                .userId(member.getId())
                .nickname(member.getNickname())
                .profileImageUrl(member.getProfileImg() != null ? imgUrl : null)
                .gender(member.getGender().name())
                .level(member.getLevel().getKoreanName())
                .build();
    }

    private int getRolePriority(String role) {
        return switch (role) {
            case "party_MANAGER" -> 0; // 모임장 역할
            case "party_SUBMANAGER" -> 1; // 부모임장
            case "party_MEMBER" -> 2; // 일반 멤버
            default -> 99;
        };
    }
}
