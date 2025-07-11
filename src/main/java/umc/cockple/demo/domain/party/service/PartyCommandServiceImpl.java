package umc.cockple.demo.domain.party.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.converter.PartyConverter;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.domain.PartyJoinRequest;
import umc.cockple.demo.domain.party.dto.*;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.domain.party.repository.PartyAddrRepository;
import umc.cockple.demo.domain.party.repository.PartyJoinRequestRepository;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.RequestStatus;
import umc.cockple.demo.global.s3.ImageService;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PartyCommandServiceImpl implements PartyCommandService{

    private final PartyRepository partyRepository;
    private final PartyAddrRepository partyAddrRepository;
    private final PartyJoinRequestRepository partyJoinRequestRepository;
    private final MemberRepository memberRepository;
    private final MemberPartyRepository memberPartyRepository;
    private final PartyConverter partyConverter;
    private final ImageService imageService;

    @Override
    public PartyCreateResponseDTO createParty(Long memberId, MultipartFile profileImage, PartyCreateRequestDTO request) {
        log.info("모임 생성 시작 - memberId: {}", memberId);

        //DTO -> Command 객체로 변환
        PartyCreateCommand partyCommand = partyConverter.toCreateCommand(request);
        PartyAddrCreateCommand addrCommand = partyConverter.toAddrCreateCommand(request);

        //모임장이 될 사용자 조회
        Member owner = findMemberOrThrow(memberId);

        //주소 처리 (조회 또는 새로 생성)
        PartyAddr partyAddr = findOrCreatePartyAddr(addrCommand);

        //이미지 업로드 처리
        String imageUrl = imageService.uploadImage(profileImage);

        //Party 엔티티 생성
        Party newParty = Party.create(partyCommand, partyAddr, imageUrl, owner);

        //DB에 Party 저장
        Party savedParty = partyRepository.save(newParty);

        log.info("모임 생성 완료 - partyId: {}", savedParty.getId());

        //ResponseDTO로 변환하여 반환
        return partyConverter.toCreateResponseDTO(savedParty);
    }

    @Override
    public PartyJoinResponseDTO createJoinRequest(Long partyId, Long memberId) {
        log.info("가입신청 시작 - partyId: {}, memberId: {}", partyId, memberId);

        //모임 및 사용자 조회
        Member member = findMemberOrThrow(memberId);
        Party party = findPartyOrThrow(partyId);

        //가입신청 가능한지 검증
        validateJoinRequest(member, party);

        //가입신청 엔티티 생성
        PartyJoinRequest newPartyJoinRequest = PartyJoinRequest.create(member, party);

        //DB에 PartyJoinRequest 저장
        PartyJoinRequest savedPartyJoinRequest = partyJoinRequestRepository.save(newPartyJoinRequest);

        log.info("가입신청 완료 - JoinRequestId: {}", savedPartyJoinRequest.getId());

        //ResponseDTO로 변환하여 반환
        return PartyConverter.toJoinResponseDTO(savedPartyJoinRequest);
    }

    //사용자 조회
    private Member findMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    //모임 조회
    private Party findPartyOrThrow(Long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new PartyException(PartyErrorCode.PARTY_NOT_FOUND));
    }

    private void validateJoinRequest(Member member, Party party) {
        //이미 가입한 멤버인지 확인
        if (memberPartyRepository.existsByPartyAndMember(party, member)) {
            throw new PartyException(PartyErrorCode.ALREADY_MEMBER);
        }
        //이미 보낸 신청이 있는지 확인
        if (partyJoinRequestRepository.existsByPartyAndMemberAndStatus(party, member, RequestStatus.PENDING) {
            throw new PartyException(PartyErrorCode.JOIN_REQUEST_ALREADY_EXISTS);
        }
    }

    //주소가 이미 존재하면 조회, 없으면 새로 생성하여 저장
    private PartyAddr findOrCreatePartyAddr(PartyAddrCreateCommand command) {
        return partyAddrRepository.findByAddr1AndAddr2(command.addr1(), command.addr2())
                .orElseGet(() -> {
                    PartyAddr newAddr = PartyAddr.create(command.addr1(), command.addr2());
                    return partyAddrRepository.save(newAddr);
                });
    }
}