package umc.cockple.demo.domain.party.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.service.ChatRoomService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.notification.dto.CreateNotificationRequestDTO;
import umc.cockple.demo.domain.notification.enums.NotificationTarget;
import umc.cockple.demo.domain.notification.service.NotificationCommandService;
import umc.cockple.demo.domain.party.converter.PartyConverter;
import umc.cockple.demo.domain.party.domain.*;
import umc.cockple.demo.domain.party.dto.*;
import umc.cockple.demo.domain.party.enums.ParticipationType;
import umc.cockple.demo.domain.party.enums.PartyStatus;
import umc.cockple.demo.domain.party.enums.RequestAction;
import umc.cockple.demo.domain.party.enums.RequestStatus;
import umc.cockple.demo.domain.party.events.PartyMemberJoinedEvent;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.domain.party.repository.*;
import umc.cockple.demo.global.enums.*;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PartyCommandServiceImpl implements PartyCommandService {

    private final PartyRepository partyRepository;
    private final PartyAddrRepository partyAddrRepository;
    private final PartyJoinRequestRepository partyJoinRequestRepository;
    private final MemberRepository memberRepository;
    private final MemberPartyRepository memberPartyRepository;
    private final PartyConverter partyConverter;
    private final PartyInvitationRepository partyInvitationRepository;
    private final PartyKeywordRepository partyKeywordRepository;

    private final NotificationCommandService notificationCommandService;
    private final ChatRoomService chatRoomService;

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public PartyCreateDTO.Response createParty(Long memberId, PartyCreateDTO.Request request) {
        log.info("모임 생성 시작 - memberId: {}", memberId);

        //DTO -> Command 객체로 변환
        PartyCreateDTO.Command partyCommand = partyConverter.toCreateCommand(request);
        PartyCreateDTO.AddrCommand addrCommand = partyConverter.toAddrCreateCommand(request);
        //모임장이 될 사용자 조회
        Member owner = findMemberOrThrow(memberId);
        //주소 처리 (조회 또는 새로 생성)
        PartyAddr partyAddr = findOrCreatePartyAddr(addrCommand);

        //모임 생성 가능한지 검증
        validateCreateParty(owner, partyCommand);

        //Party 엔티티 생성
        Party newParty = Party.create(partyCommand, partyAddr, owner);
        //DB에 Party 저장
        Party savedParty = partyRepository.save(newParty);

        chatRoomService.createPartyChatRoom(savedParty, owner);

        log.info("모임 생성 완료 - partyId: {}", savedParty.getId());

        //ResponseDTO로 변환하여 반환
        return partyConverter.toCreateResponseDTO(savedParty);
    }

    @Override
    public void updateParty(Long partyId, Long memberId, PartyUpdateDTO.Request request) {
        log.info("모임 정보 수정 시작 - partyId: {}", partyId);

        //모임 조회
        Party party = findPartyOrThrow(partyId);
        Member member = findMemberOrThrow(memberId);

        //모임장 권한 검증
        validateOwnerPermission(party, memberId);

        //비즈니스 로직 수행
        party.update(request);

        createNotification(member, partyId, NotificationTarget.PARTY_MODIFY);

        log.info("모임 정보 수정 완료 - partyId: {}", partyId);
    }

    @Override
    public void deleteParty(Long partyId, Long memberId) {
        log.info("모임 삭제 시작 - partyId: {}", partyId);

        //모임 조회
        Party party = findPartyOrThrow(partyId);
        Member member = findMemberOrThrow(memberId);

        //모임 활성화 검증
        validatePartyIsActive(party);
        //모임장 권한 검증
        validateOwnerPermission(party, memberId);

        //Party 엔티티의 상태를 INACTIVE로 변경
        party.delete();

        createNotification(member, partyId, NotificationTarget.PARTY_DELETE);

        log.info("모임 삭제 완료 - partyId: {}", partyId);
    }

    @Override
    public void leaveParty(Long partyId, Long memberId) {
        log.info("모임 탈퇴 시작 - partyId: {}, memberId: {}", partyId, memberId);

        //모임, 사용자 조회
        Party party = findPartyOrThrow(partyId);
        Member member = findMemberOrThrow(memberId);

        //모임 활성화 검증
        validatePartyIsActive(party);
        //모임장인 경우, 탈퇴가 불가능하도록 검증
        validateIsNotOwner(party, memberId);
        //해당 모임의 멤버인지 검증 및 조회
        MemberParty memberParty = findMemberPartyOrThrow(party, member);

        //모임 탈퇴 로직 수행
        memberPartyRepository.delete(memberParty);

        //채팅방 퇴장
        chatRoomService.leavePartyChatRoom(party.getId(), member.getId());

        //채팅방 퇴장 이벤트 발행
        applicationEventPublisher.publishEvent(PartyMemberJoinedEvent.left(partyId, member));
        log.info("모임 탈퇴 완료 - partyId: {}, memberId: {}", partyId, memberId);
    }

    @Override
    public void removeMember(Long partyId, Long memberIdToRemove, Long currentMemberId) {
        log.info("모임 멤버 삭제 시작 - partyId: {}, remover: {}, memberToRemove: {}", partyId, currentMemberId, memberIdToRemove);

        //모임, 사용자 조회
        Party party = findPartyOrThrow(partyId);
        Member remover = findMemberOrThrow(currentMemberId); //삭제를 요청한 사용자
        Member memberToRemove = findMemberOrThrow(memberIdToRemove); // 삭제될 사용자
        MemberParty memberPartyToRemove = findMemberPartyOrThrow(party, memberToRemove);

        //모임 활성화 검증
        validatePartyIsActive(party);
        //모임 멤버 삭제 검증
        validateRemovalPermission(party, remover, memberPartyToRemove);

        //모임 멤버 삭제 로직 수행
        memberPartyRepository.delete(memberPartyToRemove);

        //채팅방 퇴장
        chatRoomService.leavePartyChatRoom(party.getId(), memberToRemove.getId());

        //채팅방 퇴장 이벤트 발행
        applicationEventPublisher.publishEvent(PartyMemberJoinedEvent.left(partyId, memberToRemove));
        log.info("모임 멤버 삭제 완료 - partyId: {}, removed: {}", partyId, memberIdToRemove);
    }


    @Override
    public PartyJoinCreateDTO.Response createJoinRequest(Long partyId, Long memberId) {
        log.info("가입신청 시작 - partyId: {}, memberId: {}", partyId, memberId);

        //모임 및 사용자 조회
        Member member = findMemberOrThrow(memberId);
        Party party = findPartyOrThrow(partyId);

        //모임 활성화 검증
        validatePartyIsActive(party);
        //가입신청 가능한지 검증
        validateJoinRequest(member, party);

        //가입신청 엔티티 생성
        PartyJoinRequest newPartyJoinRequest = PartyJoinRequest.create(member, party);
        //DB에 PartyJoinRequest 저장
        PartyJoinRequest savedPartyJoinRequest = partyJoinRequestRepository.save(newPartyJoinRequest);

        log.info("가입신청 완료 - JoinRequestId: {}", savedPartyJoinRequest.getId());

        //ResponseDTO로 변환하여 반환
        return partyConverter.toJoinResponseDTO(savedPartyJoinRequest);
    }

    @Override
    public void actionJoinRequest(Long partyId, Long memberId, PartyJoinActionDTO.Request request, Long requestId) {
        log.info("가입신청 처리 시작 - partyId: {}, memberId: {}, requestId: {}", partyId, memberId, requestId);

        //모임, 가입신청 조회
        Party party = findPartyOrThrow(partyId);
        PartyJoinRequest partyJoinRequest = findJoinRequestOrThrow(requestId);
        Member member = partyJoinRequest.getMember();

        //모임 활성화 검증
        validatePartyIsActive(party);
        //모임장 권한 검증
        validateOwnerPermission(party, memberId);
        //가입신청 처리 가능한지 검증
        validateJoinRequestAction(party, partyJoinRequest, member);

        //비즈니스 로직 수행 (승인/거절에 따른 처리)
        if (RequestAction.APPROVE.equals(request.action())) {
            approveJoinRequest(partyJoinRequest);
        } else {
            rejectJoinRequest(partyJoinRequest);
        }

        log.info("가입신청 처리 완료 - requestId: {}", requestId);
    }

    @Override
    public PartyInviteCreateDTO.Response createInvitation(Long partyId, Long memberIdToInvite, Long currentMemberId) {
        log.info("멤버 초대 시작 - partyId: {}, inviterId: {}, inviteeId: {}", partyId, currentMemberId, memberIdToInvite);

        //모임, 사용자 조회
        Party party = findPartyOrThrow(partyId);
        Member inviter = findMemberOrThrow(currentMemberId);
        Member invitee = findMemberOrThrow(memberIdToInvite);

        //멤버 초대 보내기 검증
        validateInvitation(party, inviter, invitee);

        PartyInvitation newInvitation = PartyInvitation.create(party, inviter, invitee);
        PartyInvitation savedPartyInvitation = partyInvitationRepository.save(newInvitation);

        createInviteNotification(invitee, partyId, NotificationTarget.PARTY_INVITE, savedPartyInvitation.getId());

        log.info("멤버 초대 완료 - PartyInvitation: {}", savedPartyInvitation.getId());
        return partyConverter.toInviteResponseDTO(savedPartyInvitation);
    }

    @Override
    public void actionInvitation(Long memberId, PartyInviteActionDTO.Request request, Long invitationId) {
        log.info("모임 초대 처리 시작 - memberId: {}, invitationId: {}", memberId, invitationId);

        //모임 초대 조회
        PartyInvitation invitation = findInvitationOrThrow(invitationId);
        Member invitee = findMemberOrThrow(memberId);

        //모임 초대 처리 검증
        validateInvitationAction(invitation, memberId);

        //비즈니스 로직 수행 (승인/거절에 따른 처리)
        if (RequestAction.APPROVE.equals(request.action())) {
            approveInvitation(invitation);
            createInviteApprovedNotification(invitation.getInviter(), invitation.getParty().getId(), NotificationTarget.PARTY_INVITE_APPROVED, invitee);
        } else {
            rejectInvitation(invitation);
        }

        log.info("모임 초대 처리 완료 - invitationId: {}", invitationId);
    }

    @Override
    public void addKeyword(Long partyId, Long memberId, PartyKeywordDTO.Request request) {
        log.info("키워드 추가 시작 - partyId: {}", partyId);
        //모임 조회, 키워드 변환
        Party party = findPartyOrThrow(partyId);
        List<Keyword> keywords = request.keywords().stream()
                .map(Keyword::fromKorean)
                .toList();

        //모임장 권한 검증
        validateOwnerPermission(party, memberId);

        //키워드 추가
        party.updateKeywords(keywords);
        log.info("키워드 추가 완료 - partyId: {}", partyId);
    }

    // ========== 조회 메서드 ==========
    //가입신청 조회
    private PartyJoinRequest findJoinRequestOrThrow(Long requestId) {
        return partyJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new PartyException(PartyErrorCode.JoinRequest_NOT_FOUND));
    }

    private PartyInvitation findInvitationOrThrow(Long invitationId) {
        return partyInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new PartyException(PartyErrorCode.INVITATION_NOT_FOUND));
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

    //모임 멤버 조회
    private MemberParty findMemberPartyOrThrow(Party party, Member member) {
        return memberPartyRepository.findByPartyAndMember(party, member)
                .orElseThrow(() -> new PartyException(PartyErrorCode.NOT_MEMBER));
    }

    //주소가 이미 존재하면 조회, 없으면 새로 생성하여 저장
    private PartyAddr findOrCreatePartyAddr(PartyCreateDTO.AddrCommand command) {
        return partyAddrRepository.findByAddr1AndAddr2(command.addr1(), command.addr2())
                .orElseGet(() -> {
                    PartyAddr newAddr = PartyAddr.create(command.addr1(), command.addr2());
                    return partyAddrRepository.save(newAddr);
                });
    }

    // ========== 검증 메서드 ==========
    //모임장 권한 검증
    private void validateOwnerPermission(Party party, Long memberId) {
        if (!party.getOwnerId().equals(memberId)) {
            throw new PartyException(PartyErrorCode.INSUFFICIENT_PERMISSION);
        }
    }

    //모임장은 권한이 없음을 검증
    private void validateIsNotOwner(Party party, Long memberId) {
        if (party.getOwnerId().equals(memberId)) {
            throw new PartyException(PartyErrorCode.INVALID_ACTION_FOR_OWNER);
        }
    }

    //모임 생성 검증
    private void validateCreateParty(Member owner, PartyCreateDTO.Command command) {
        ParticipationType partyType = command.partyType();
        Gender ownerGender = owner.getGender();
        Level ownerLevel = owner.getLevel();

        // 혼복인 경우, 남녀 급수 정보가 모두 있는지 검증
        if (partyType == ParticipationType.MIX_DOUBLES) {
            //FEMALE은 DTO단에서 검증 완료
            if (command.maleLevel() == null || command.maleLevel().isEmpty()) {
                throw new PartyException(PartyErrorCode.MALE_LEVEL_REQUIRED);
            }
        }

        //여복인 경우, 남자 급수 설정 방지
        if (partyType == ParticipationType.WOMEN_DOUBLES && command.maleLevel() != null && !command.maleLevel().isEmpty()) {
            throw new PartyException(PartyErrorCode.MALE_LEVEL_NOT_NEEDED);
        }

        //생성하려는 모임의 모임 유형의 성별에 본인도 적합한지 확인
        validateGenderRequirement(ownerGender, partyType);

        //생성하려는 모임의 나이 조건에 본인도 적합한지 확인
        validateAgeRequirement(owner, command.minBirthYear(), command.maxBirthYear());
    }

    //모임 멤버 삭제 검증
    private void validateRemovalPermission(Party party, Member remover, MemberParty memberPartyToRemove) {
        //자기 자신을 삭제하려는 경우
        if (remover.getId().equals(memberPartyToRemove.getMember().getId())) {
            //부모임장인 경우에만 가능
            MemberParty removerMemberParty = findMemberPartyOrThrow(party, remover);
            if (removerMemberParty.getRole() == Role.party_SUBMANAGER) {
                return;
            } else {
                throw new PartyException(PartyErrorCode.CANNOT_REMOVE_SELF);
            }
        }

        //다른 사람을 삭제하려는 경우
        MemberParty removerMemberParty = findMemberPartyOrThrow(party, remover);
        Role removerRole = removerMemberParty.getRole();
        Role targetRole = memberPartyToRemove.getRole();
        //모임장은 모두 삭제 가능
        if (removerRole == Role.party_MANAGER) {
            return;
        }
        //부모임장은 일반 멤버만 삭제 가능 (모임장을 삭제하려할 경우 권한 없음)
        if (removerRole == Role.party_SUBMANAGER && targetRole == Role.party_MEMBER) {
            return;
        }
        //일반 멤버는 권한 없음
        throw new PartyException(PartyErrorCode.INSUFFICIENT_PERMISSION);
    }

    //모임 가입신청 검증
    private void validateJoinRequest(Member member, Party party) {
        //이미 가입한 멤버인지 검증
        if (memberPartyRepository.existsByPartyAndMember(party, member)) {
            throw new PartyException(PartyErrorCode.ALREADY_MEMBER);
        }
        //이미 보낸 신청이 있는지 검증
        if (partyJoinRequestRepository.existsByPartyAndMemberAndStatus(party, member, RequestStatus.PENDING)) {
            throw new PartyException(PartyErrorCode.JOIN_REQUEST_ALREADY_EXISTS);
        }
        //해당 모임의 모임 유형에 맞는 성별인지 검증
        validateGenderRequirement(member.getGender(), party.getPartyType());

        //해당 모임의 나이 조건에 적합한지 검증
        validateAgeRequirement(member, party.getMinBirthYear(), party.getMaxBirthYear());
    }

    //나이대 검증
    private void validateAgeRequirement(Member member, Integer minBirthYear, Integer maxBirthYear) {
        Integer memberBirthYear = member.getBirth().getYear();

        if (minBirthYear > memberBirthYear || memberBirthYear > maxBirthYear) {
            throw new PartyException(PartyErrorCode.AGE_NOT_MATCH);
        }
    }

    //성별 검증
    private void validateGenderRequirement(Gender ownerGender, ParticipationType partyType) {
        if (partyType == ParticipationType.WOMEN_DOUBLES && ownerGender != Gender.FEMALE) {
            throw new PartyException(PartyErrorCode.GENDER_NOT_MATCH);
        }
    }

    //모임 가입신청 처리 검증
    private void validateJoinRequestAction(Party party, PartyJoinRequest joinRequest, Member member) {
        //해당 모임의 가입신청인지 검증
        if (!joinRequest.getParty().getId().equals(party.getId())) {
            throw new PartyException(PartyErrorCode.JOIN_REQUEST_PARTY_NOT_FOUND);
        }

        //이미 멤버인지 검증
        if (memberPartyRepository.existsByPartyAndMember(party, member)) {
            throw new PartyException(PartyErrorCode.ALREADY_MEMBER);
        }

        //이미 처리된 가입신청인지 검증
        if (joinRequest.getStatus() != RequestStatus.PENDING) {
            throw new PartyException(PartyErrorCode.JOIN_REQUEST_ALREADY_ACTIONS);
        }
    }

    //모임 초대 보내기 검증
    private void validateInvitation(Party party, Member inviter, Member invitee) {
        //모임장 권한 검증
        validateOwnerPermission(party, inviter.getId());

        //이미 가입한 멤버인지 검증
        if (memberPartyRepository.existsByPartyAndMember(party, invitee)) {
            throw new PartyException(PartyErrorCode.ALREADY_MEMBER);
        }

        //이미 처리 대기중인 초대가 있는지 검증
        if (partyInvitationRepository.existsByPartyAndInviteeAndStatus(party, invitee, RequestStatus.PENDING)) {
            throw new PartyException(PartyErrorCode.INVITATION_ALREADY_EXISTS);
        }
    }

    //모임 초대 처리 검증
    private void validateInvitationAction(PartyInvitation invitation, Long memberId) {
        //초대받은 사용자와 현재 사용자가 일치하는지 검증
        if (!memberId.equals(invitation.getInvitee().getId())) {
            throw new PartyException(PartyErrorCode.NOT_YOUR_INVITATION);
        }

        //이미 멤버인지 검증
        if (memberPartyRepository.existsByPartyAndMember(invitation.getParty(), invitation.getInvitee())) {
            throw new PartyException(PartyErrorCode.ALREADY_MEMBER);
        }

        //이미 처리된 모임 초대인지 검증
        if (invitation.getStatus() != RequestStatus.PENDING) {
            throw new PartyException(PartyErrorCode.INVITATION_ALREADY_ACTIONS);
        }
    }

    //모임 활성화 여부 검증
    private void validatePartyIsActive(Party party) {
        if (party.getStatus() == PartyStatus.INACTIVE) {
            throw new PartyException(PartyErrorCode.PARTY_IS_DELETED);
        }
    }

    // ========== 비즈니스 로직 메서드 ==========
    //가입 신청 승인, 승인
    private void approveJoinRequest(PartyJoinRequest partyJoinRequest) {
        partyJoinRequest.updateStatus(RequestStatus.APPROVED);
        Party party = partyJoinRequest.getParty();
        Member member = partyJoinRequest.getMember();
        MemberParty newMemberParty = MemberParty.create(party, member);
        party.addMember(newMemberParty);

        chatRoomService.joinPartyChatRoom(party.getId(), member);
        applicationEventPublisher.publishEvent(
                PartyMemberJoinedEvent.joined(party.getId(), member)
        );
    }

    private void rejectJoinRequest(PartyJoinRequest partyJoinRequest) {
        partyJoinRequest.updateStatus(RequestStatus.REJECTED);
    }

    //모임 초대 승인, 거절
    private void approveInvitation(PartyInvitation invitation) {
        invitation.updateStatus(RequestStatus.APPROVED);
        Party party = invitation.getParty();
        Member member = invitation.getInvitee();
        MemberParty newMemberParty = MemberParty.create(party, member);
        party.addMember(newMemberParty);

        chatRoomService.joinPartyChatRoom(party.getId(), member);
        applicationEventPublisher.publishEvent(
                PartyMemberJoinedEvent.joined(party.getId(), member)
        );
    }

    private void rejectInvitation(PartyInvitation invitation) {
        invitation.updateStatus(RequestStatus.REJECTED);
    }

    //알림 생성
    private void createNotification(Member member, Long partyId, NotificationTarget notificationTarget) {
        CreateNotificationRequestDTO dto = CreateNotificationRequestDTO.builder()
                .member(member)
                .partyId(partyId)
                .target(notificationTarget)
                .build();
        notificationCommandService.createNotification(dto);
    }

    //알림 생성 (INVITE 타입)
    private void createInviteNotification(Member member, Long partyId, NotificationTarget notificationTarget, Long inviteId) {
        CreateNotificationRequestDTO dto = CreateNotificationRequestDTO.builder()
                .member(member)
                .partyId(partyId)
                .target(notificationTarget)
                .invitationId(inviteId)
                .build();
        notificationCommandService.createNotification(dto);
    }

    private void createInviteApprovedNotification(Member member, Long partyId, NotificationTarget notificationTarget, Member invitee) {
        CreateNotificationRequestDTO dto = CreateNotificationRequestDTO.builder()
                .member(member)
                .partyId(partyId)
                .target(notificationTarget)
                .subjectName(invitee.getNickname())
                .build();
        notificationCommandService.createNotification(dto);
    }
}