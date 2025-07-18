package umc.cockple.demo.domain.party.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.converter.PartyConverter;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyJoinRequest;
import umc.cockple.demo.domain.party.dto.PartyDetailDTO;
import umc.cockple.demo.domain.party.dto.PartyJoinDTO;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.domain.party.repository.PartyJoinRequestRepository;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.RequestStatus;

import java.util.Optional;

@Service
//조회 용 서비스이기에 readOnly = true를 추가하여 성능 향상했습니다.
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PartyQueryServiceImpl implements PartyQueryService{
    private final PartyRepository partyRepository;
    private final PartyJoinRequestRepository partyJoinRequestRepository;
    private final PartyConverter partyConverter;
    private final MemberRepository memberRepository;
    private final MemberPartyRepository memberPartyRepository;

    @Override
    public PartyDetailDTO.Response getPartyDetails(Long partyId, Long memberId) {
        log.info("모임 상세 정보 조회 시작 - partyId: {}, memberId: {}", partyId, memberId);

        //모임, 사용자 조회
        Party party = findPartyOrThrow(partyId);
        Member member = findMemberOrThrow(memberId);

        //memberParty 반환
        Optional<MemberParty> memberParty = memberPartyRepository.findByPartyAndMember(party, member);

        PartyDetailDTO.Response response = partyConverter.toPartyDetailResponseDTO(party, memberParty);

        log.info("모임 상세 정보 조회 완료 - partyId: {}", partyId);
        return response;
    }

    @Override
    public Slice<PartyJoinDTO.Response> getJoinRequests(Long partyId, Long memberId, String status, Pageable pageable) {
        log.info("가입 신청 목록 조회 시작 - partyId: {}, memberId: {}", partyId, memberId);

        //모임 조회
        Party party = findPartyOrThrow(partyId);
        //모임장 권한이 있는지 확인
        validateOwnerPermission(party, memberId);
        //status를 ENUM으로 변환 및 검증
        RequestStatus requestStatus = parseRequestStatus(status);

        //조회 로직 수행
        Slice<PartyJoinRequest> requestSlice = partyJoinRequestRepository
                .findByPartyAndStatus(party, requestStatus, pageable);

        log.info("가입 신청 목록 조회 완료. 조회된 항목 수: {}", requestSlice.getNumberOfElements());
        return requestSlice.map(partyConverter::toPartyJoinResponseDTO);
    }

    //모임장 권한 확인
    private void validateOwnerPermission(Party party, Long memberId) {
        if(!party.getOwnerId().equals(memberId)){
            throw new PartyException(PartyErrorCode.INSUFFICIENT_PERMISSION);
        }
    }

    //사용자 조회
    private Party findPartyOrThrow(Long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new PartyException(PartyErrorCode.PARTY_NOT_FOUND));
    }

    //멤버 조회
    private Member findMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    private RequestStatus parseRequestStatus(String status) {
        try {
            return RequestStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PartyException(PartyErrorCode.INVALID_REQUEST_STATUS);
        }
    }
}
