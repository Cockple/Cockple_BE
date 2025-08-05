package umc.cockple.demo.domain.exercise.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.converter.ExerciseConverter;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.*;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberExercise;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.domain.party.enums.PartyStatus;
import umc.cockple.demo.global.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ExerciseCommandService {

    private final ExerciseRepository exerciseRepository;
    private final PartyRepository partyRepository;
    private final MemberPartyRepository memberPartyRepository;
    private final MemberRepository memberRepository;
    private final MemberExerciseRepository memberExerciseRepository;
    private final GuestRepository guestRepository;

    private final ExerciseValidator exerciseValidator;

    private final ExerciseConverter exerciseConverter;

    public ExerciseCreateDTO.Response createExercise(Long partyId, Long memberId, ExerciseCreateDTO.Request request) {
        log.info("운동 생성 시작 - partyId: {}, memberId: {}, date: {}", partyId, memberId, request.date());

        Party party = findPartyOrThrow(partyId);
        exerciseValidator.validateCreateExercise(memberId, request, party);

        ExerciseCreateDTO.Command exerciseCommand = exerciseConverter.toCreateCommand(request);
        ExerciseCreateDTO.AddrCommand addrCommand = exerciseConverter.toAddrCreateCommand(request);

        Exercise exercise = party.createExercise(exerciseCommand, addrCommand);
        party.addExercise(exercise);

        Exercise savedExercise = exerciseRepository.save(exercise);

        log.info("운동 생성 완료 - 운동ID: {}", savedExercise.getId());

        return exerciseConverter.toCreateResponse(savedExercise);
    }

    public ExerciseJoinDTO.Response joinExercise(Long exerciseId, Long memberId) {

        log.info("운동 신청 시작 - exerciseId: {}, memberId: {}", exerciseId, memberId);

        Exercise exercise = findExerciseWithPartyLevelOrThrow(exerciseId);
        Member member = findMemberOrThrow(memberId);
        validateJoinExercise(exercise, member);

        boolean isPartyMember = isPartyMember(exercise, member);
        MemberExercise memberExercise = MemberExercise.create(isPartyMember);
        member.addParticipation(memberExercise);
        exercise.addParticipation(memberExercise);

        MemberExercise savedMemberExercise = memberExerciseRepository.save(memberExercise);

        log.info("운동 신청 종료 - memberExerciseId: {}, isPartyMember : {}"
                , savedMemberExercise.getId(), isPartyMember);

        return exerciseConverter.toJoinResponse(savedMemberExercise, exercise);
    }

    public ExerciseGuestInviteDTO.Response inviteGuest(Long exerciseId, Long inviterId, ExerciseGuestInviteDTO.Request request) {

        log.info("게스트 초대 시작 - exerciseId: {}, inviterId: {}, guestName: {}"
                , exerciseId, inviterId, request.guestName());

        Exercise exercise = findExerciseOrThrow(exerciseId);
        Member inviter = findMemberOrThrow(inviterId);
        validateGuestInvitation(exercise, inviter);

        ExerciseGuestInviteDTO.Command command = exerciseConverter.toGuestInviteCommand(request, inviterId);

        Guest guest = Guest.create(command);
        exercise.addGuest(guest);

        Guest savedGuest = guestRepository.save(guest);

        log.info("게스트 초대 완료 - guestId: {}", savedGuest.getId());

        return exerciseConverter.toGuestInviteResponse(savedGuest, exercise);
    }

    public ExerciseCancelDTO.Response cancelParticipation(Long exerciseId, Long memberId) {

        log.info("운동 참여 취소 시작 - exerciseId: {}, memberId: {}", exerciseId, memberId);

        Exercise exercise = findExerciseOrThrow(exerciseId);
        Member member = findMemberOrThrow(memberId);
        MemberExercise memberExercise = findMemberExerciseOrThrow(exercise, member);
        validateCancelParticipation(exercise);

        exercise.removeParticipation(memberExercise);
        member.removeParticipation(memberExercise);

        memberExerciseRepository.delete(memberExercise);

        exerciseRepository.save(exercise);

        log.info("운동 참여 취소 완료 - exerciseId: {}, memberId: {}, 현재 참여자 수: {}",
                exerciseId, memberId, exercise.getNowCapacity());

        return exerciseConverter.toCancelResponse(exercise, member);
    }

    public ExerciseCancelDTO.Response cancelGuestInvitation(Long exerciseId, Long guestId, Long memberId) {

        log.info("게스트 초대 취소 시작 - exerciseId: {}, guestId: {}, memberId: {}", exerciseId, guestId, memberId);

        Exercise exercise = findExerciseOrThrow(exerciseId);
        Member member = findMemberOrThrow(memberId);
        Guest guest = findGuestOrThrow(guestId);
        validateCancelGuestInvitation(exercise, guest, member);

        exercise.removeGuest(guest);

        guestRepository.delete(guest);

        exerciseRepository.save(exercise);

        log.info("게스트 초대 취소 완료 - exerciseId: {}, guestId: {}, memberId: {}", exerciseId, guestId, memberId);

        return exerciseConverter.toCancelResponse(exercise, guest);
    }

    public ExerciseCancelDTO.Response cancelParticipationByManager(
            Long exerciseId, Long participantId, Long memberId, ExerciseCancelDTO.ByManagerRequest request) {

        log.info("매니저에 의한 운동 참여 취소 시작 - exerciseId: {}, participantId: {}, memberId: {}", exerciseId, participantId, memberId);

        Exercise exercise = findExerciseOrThrow(exerciseId);
        Member manager = findMemberOrThrow(memberId);
        validateCancelParticipationByManager(exercise, manager);

        ExerciseCancelDTO.Response response = executeParticipantCancellation(exercise, participantId, request);

        log.info("매니저에 의한 운동 참여 취소 완료 - exerciseId: {}, participantId: {}, 현재 참여자 수: {}",
                exerciseId, participantId, exercise.getNowCapacity());

        return response;
    }

    public ExerciseDeleteDTO.Response deleteExercise(Long exerciseId, Long memberId) {

        log.info("운동 삭제 시작 - exerciseId: {}, memberId: {}", exerciseId, memberId);

        Exercise exercise = findExerciseOrThrow(exerciseId);
        validateDeleteExercise(exercise, memberId);

        Party party = exercise.getParty();
        party.removeExercise(exercise);
        exerciseRepository.delete(exercise);

        partyRepository.save(party);

        log.info("운동 삭제 종료 - exerciseId: {}, memberId: {}", exerciseId, memberId);

        return exerciseConverter.toDeleteResponse(exercise);
    }

    public ExerciseUpdateDTO.Response updateExercise(Long exerciseId, Long memberId, ExerciseUpdateDTO.Request request) {

        log.info("운동 업데이트 시작 - exerciseId: {}, memberId: {}", exerciseId, memberId);

        Exercise exercise = findExerciseOrThrow(exerciseId);
        Member member = findMemberOrThrow(memberId);
        validateUpdateExercise(exercise, member, request);

        ExerciseUpdateDTO.Command updateCommand = exerciseConverter.toUpdateCommand(request);
        ExerciseUpdateDTO.AddrCommand addrUpdateCommand = exerciseConverter.toAddrUpdateCommand(request);

        exercise.updateExerciseInfo(updateCommand);
        exercise.updateExerciseAddr(addrUpdateCommand);

        Exercise savedExercise = exerciseRepository.save(exercise);

        log.info("운동 수정 완료 - exerciseId: {}", savedExercise.getId());

        return exerciseConverter.toUpdateResponse(savedExercise);
    }


    // ========== 검증 메서드들 ==========

    private void validateJoinExercise(Exercise exercise, Member member) {
        validateAlreadyStarted(exercise, ExerciseErrorCode.EXERCISE_ALREADY_STARTED_PARTICIPATION);
        validateAlreadyJoined(exercise, member);
        validateJoinPermission(exercise, member);
        validateMemberLevel(exercise.getParty(), member);
        validateMemberAge(exercise.getParty(), member);
    }

    private void validateGuestInvitation(Exercise exercise, Member inviter) {
        validateAlreadyStarted(exercise, ExerciseErrorCode.EXERCISE_ALREADY_STARTED_INVITATION);
        validateInviterIsPartyMember(exercise, inviter);
        validateGuestPolicy(exercise);
    }

    private void validateCancelParticipation(Exercise exercise) {
        validateAlreadyStarted(exercise, ExerciseErrorCode.EXERCISE_ALREADY_STARTED_CANCEL);
    }

    private void validateCancelGuestInvitation(Exercise exercise, Guest guest, Member member) {
        validateAlreadyStarted(exercise, ExerciseErrorCode.EXERCISE_ALREADY_STARTED_CANCEL);
        validateGuestBelongsToExercise(guest, exercise);
        validateGuestInvitedByMember(guest, member);
    }

    private void validateCancelParticipationByManager(Exercise exercise, Member manager) {
        validateAlreadyStarted(exercise, ExerciseErrorCode.EXERCISE_ALREADY_STARTED_CANCEL);
        validateMemberPermission(manager.getId(), exercise.getParty());
    }

    private void validateDeleteExercise(Exercise exercise, Long memberId) {
        validateMemberPermission(memberId, exercise.getParty());
    }

    private void validateUpdateExercise(Exercise exercise, Member member, ExerciseUpdateDTO.Request request) {
        validateMemberPermission(member.getId(), exercise.getParty());
        validateAlreadyStarted(exercise, ExerciseErrorCode.EXERCISE_ALREADY_STARTED_UPDATE);
        validateUpdateTime(request, exercise);
    }

    // ========== 세부 검증 메서드들 ==========

    private void validateMemberPermission(Long memberId, Party party) {
        boolean isOwner = party.getOwnerId().equals(memberId);
        boolean isManager = memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                party.getId(), memberId, Role.party_MANAGER);

        if (!isOwner && !isManager)
            throw new ExerciseException(ExerciseErrorCode.INSUFFICIENT_PERMISSION);
    }

    private void validateAlreadyStarted(Exercise exercise, ExerciseErrorCode errorCode) {
        if (exercise.isAlreadyStarted()) {
            throw new ExerciseException(errorCode);
        }
    }

    private void validateAlreadyJoined(Exercise exercise, Member member) {
        if(memberExerciseRepository.existsByExerciseAndMember(exercise, member)) {
            throw new ExerciseException(ExerciseErrorCode.ALREADY_JOINED_EXERCISE);
        }
    }

    private void validateJoinPermission(Exercise exercise, Member member) {
        if(isPartyMember(exercise, member)) {
            return;
        }

        if(Boolean.FALSE.equals(exercise.getOutsideGuestAccept())) {
            throw new ExerciseException(ExerciseErrorCode.NOT_PARTY_MEMBER);
        }
    }

    private void validateMemberLevel(Party party, Member member) {
        boolean isLevelAllowed = party.getLevels().stream()
                .anyMatch(pl -> pl.getGender() == member.getGender() &&
                        pl.getLevel() == member.getLevel());

        if (!isLevelAllowed) {
            throw new ExerciseException(ExerciseErrorCode.MEMBER_LEVEL_NOT_ALLOWED);
        }
    }

    private void validateMemberAge(Party party, Member member) {
        if(!party.isAgeValid(member)){
            throw new ExerciseException(ExerciseErrorCode.MEMBER_AGE_NOT_ALLOWED);
        }
    }

    private void validateInviterIsPartyMember(Exercise exercise, Member inviter) {
        Party party = exercise.getParty();
        boolean isPartyMember = memberPartyRepository.existsByPartyAndMember(party, inviter);

        if (!isPartyMember) {
            throw new ExerciseException(ExerciseErrorCode.NOT_PARTY_MEMBER_FOR_GUEST_INVITE);
        }
    }

    private void validateGuestPolicy(Exercise exercise) {
        if (Boolean.FALSE.equals(exercise.getPartyGuestAccept())) {
            throw new ExerciseException(ExerciseErrorCode.GUEST_INVITATION_NOT_ALLOWED);
        }
    }

    private void validateGuestBelongsToExercise(Guest guest, Exercise exercise) {
        if (!guest.getExercise().getId().equals(exercise.getId())) {
            throw new ExerciseException(ExerciseErrorCode.GUEST_IS_NOT_PARTICIPATED_IN_EXERCISE);
        }
    }

    private void validateGuestInvitedByMember(Guest guest, Member member) {
        if (!guest.getInviterId().equals(member.getId())) {
            throw new ExerciseException(ExerciseErrorCode.GUEST_NOT_INVITED_BY_MEMBER);
        }
    }

    private void validateUpdateTime(ExerciseUpdateDTO.Request request, Exercise exercise) {
        LocalTime newStartTime = request.toParsedStartTime();
        LocalTime newEndTime = request.toParsedEndTime();
        LocalDate newDate = request.toParsedDate();

        LocalTime currentStartTime = exercise.getStartTime();
        LocalTime currentEndTime = exercise.getEndTime();
        LocalDate currentDate = exercise.getDate();

        LocalTime startTime = newStartTime != null ? newStartTime : currentStartTime;
        LocalTime endTime = newEndTime != null ? newEndTime : currentEndTime;
        LocalDate date = newDate != null ? newDate : currentDate;

        if (endTime != null && !startTime.isBefore(endTime)) {
            throw new ExerciseException(ExerciseErrorCode.INVALID_EXERCISE_TIME);
        }

        LocalDateTime exerciseDateTime = LocalDateTime.of(date, startTime);
        if (exerciseDateTime.isBefore(LocalDateTime.now())) {
            throw new ExerciseException(ExerciseErrorCode.PAST_TIME_NOT_ALLOWED);
        }
    }

    // ========== 비즈니스 로직 ==========

    private boolean isPartyMember(Exercise exercise, Member member) {
        Party party = exercise.getParty();
        return memberPartyRepository.existsByPartyAndMember(party, member);
    }

    private ExerciseCancelDTO.Response executeParticipantCancellation(Exercise exercise, Long participantId, ExerciseCancelDTO.ByManagerRequest request) {
        if(request.isGuest()){
            log.info("게스트 참여 취소 실행 - participantId: {}", participantId);
            return cancelGuestParticipation(exercise, participantId);
        }

        log.info("멤버 참여 취소 실행 - participantId: {}", participantId);
        return cancelMemberParticipation(exercise, participantId);
    }

    private ExerciseCancelDTO.Response cancelGuestParticipation(Exercise exercise, Long participantId) {
        Guest guest = findGuestOrThrow(participantId);
        validateGuestBelongsToExercise(guest, exercise);

        exercise.removeGuest(guest);

        guestRepository.delete(guest);

        exerciseRepository.save(exercise);

        return exerciseConverter.toCancelResponse(exercise, guest);
    }

    private ExerciseCancelDTO.Response cancelMemberParticipation(Exercise exercise, Long participantId) {
        Member participant = findMemberOrThrow(participantId);
        MemberExercise memberExercise = findMemberExerciseOrThrow(exercise, participant);

        exercise.removeParticipation(memberExercise);
        participant.removeParticipation(memberExercise);

        memberExerciseRepository.delete(memberExercise);

        exerciseRepository.save(exercise);

        return exerciseConverter.toCancelResponse(exercise, participant);
    }

    // ========== 조회 메서드 ==========

    private Exercise findExerciseOrThrow(Long exerciseId) {
        return exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.EXERCISE_NOT_FOUND));
    }

    private Exercise findExerciseWithPartyLevelOrThrow(Long exerciseId) {
        return exerciseRepository.findByIdWithPartyLevels(exerciseId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.EXERCISE_NOT_FOUND));
    }

    private Guest findGuestOrThrow(Long guestId) {
        return guestRepository.findById(guestId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.GUEST_NOT_FOUND));
    }

    private Member findMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.MEMBER_NOT_FOUND));
    }

    private MemberExercise findMemberExerciseOrThrow(Exercise exercise, Member member) {
        return memberExerciseRepository.findByExerciseAndMember(exercise, member)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.MEMBER_EXERCISE_NOT_FOUND));
    }

    private Party findPartyOrThrow(Long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.PARTY_NOT_FOUND));
    }

}
