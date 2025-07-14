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
import umc.cockple.demo.domain.party.repository.PartyRepository;
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
    private final ExerciseConverter exerciseConverter;

    public ExerciseCreateResponseDTO createExercise(Long partyId, Long memberId, ExerciseCreateRequestDTO request) {
        log.info("운동 생성 시작 - partyId: {}, memberId: {}, date: {}", partyId, memberId, request.date());

        Party party = findPartyOrThrow(partyId);
        validateCreateExercise(memberId, request, party);

        ExerciseCreateCommand exerciseCommand = exerciseConverter.toCreateCommand(request);
        ExerciseAddrCreateCommand addrCommand = exerciseConverter.toAddrCreateCommand(request);

        Exercise exercise = party.createExercise(exerciseCommand, addrCommand);
        Exercise savedExercise = exerciseRepository.save(exercise);

        log.info("운동 생성 완료 - 운동ID: {}", savedExercise.getId());

        return exerciseConverter.toCreateResponseDTO(savedExercise);
    }

    public ExerciseJoinResponseDTO joinExercise(Long exerciseId, Long memberId) {

        log.info("운동 신청 시작 - exerciseId: {}, memberId: {}", exerciseId, memberId);

        Exercise exercise = findExerciseOrThrow(exerciseId);
        Member member = findMemberOrThrow(memberId);
        validateJoinExercise(exercise, member);

        MemberExercise memberExercise = exercise.addParticipant(member);
        MemberExercise savedMemberExercise = memberExerciseRepository.save(memberExercise);

        log.info("운동 신청 종료 - memberExerciseId: {}", savedMemberExercise.getId());

        return exerciseConverter.toJoinResponseDTO(savedMemberExercise, exercise);
    }

    public GuestInviteResponseDTO inviteGuest(Long exerciseId, Long inviterId, GuestInviteRequestDTO request) {

        log.info("게스트 초대 시작 - exerciseId: {}, inviterId: {}, guestName: {}"
                , exerciseId, inviterId, request.guestName());

        Exercise exercise = findExerciseOrThrow(exerciseId);
        Member inviter = findMemberOrThrow(inviterId);
        validateGuestInvitation(exercise, inviter);

        GuestInviteCommand command = exerciseConverter.toGuestInviteCommand(request, inviterId);

        Guest guest = exercise.addGuest(command);
        Guest savedGuest = guestRepository.save(guest);

        log.info("게스트 초대 완료 - guestId: {}", savedGuest.getId());

        return exerciseConverter.toGuestInviteResponseDTO(savedGuest, exercise);
    }

    public ExerciseCancelResponseDTO cancelParticipation(Long exerciseId, Long memberId) {

        log.info("운동 참여 취소 시작 - exerciseId: {}, memberId: {}", exerciseId, memberId);

        Exercise exercise = findExerciseOrThrow(exerciseId);
        Member member = findMemberOrThrow(memberId);
        validateCancelParticipation(exercise, member);
    }

    private void validateCreateExercise(Long memberId, ExerciseCreateRequestDTO request, Party party) {
        validateMemberPermission(memberId, party);
        validateExerciseTime(request);
    }

    private void validateMemberPermission(Long memberId, Party party) {
        boolean isOwner = party.getOwnerId().equals(memberId);
        boolean isManager = memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                party.getId(), memberId, Role.party_MANAGER);

        if (!isOwner && !isManager)
            throw new ExerciseException(ExerciseErrorCode.INSUFFICIENT_PERMISSION);
    }

    private void validateExerciseTime(ExerciseCreateRequestDTO request) {
        LocalDate date = request.toParsedDate();
        LocalTime startTime = request.toParsedStartTime();
        LocalTime endTime = request.toParsedEndTime();

        if (!startTime.isBefore(endTime)) {
            throw new ExerciseException(ExerciseErrorCode.INVALID_EXERCISE_TIME);
        }

        LocalDateTime exerciseDateTime = LocalDateTime.of(date, startTime);
        if (exerciseDateTime.isBefore(LocalDateTime.now())) {
            throw new ExerciseException(ExerciseErrorCode.PAST_TIME_NOT_ALLOWED);
        }
    }

    private void validateJoinExercise(Exercise exercise, Member member) {
        validateExerciseNotStarted(exercise);
        validateAlreadyJoined(exercise, member);
        validateJoinPermission(exercise, member);
    }

    private void validateExerciseNotStarted(Exercise exercise) {
        LocalDateTime exerciseDateTime = LocalDateTime.of(exercise.getDate(), exercise.getStartTime());
        if (exerciseDateTime.isBefore(LocalDateTime.now())) {
            throw new ExerciseException(ExerciseErrorCode.EXERCISE_ALREADY_STARTED);
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

    private boolean isPartyMember(Exercise exercise, Member member) {
        Party party = exercise.getParty();
        return memberPartyRepository.existsByPartyAndMember(party, member);
    }

    private void validateGuestInvitation(Exercise exercise, Member inviter) {
        validateExerciseNotStarted(exercise);
        validateInviterIsPartyMember(exercise, inviter);
        validateGuestPolicy(exercise);
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

    private void validateCancelParticipation(Exercise exercise, Member member) {
        validateAlreadyStarted(exercise);
        validateIsJoinedExercise(exercise, member);
    }

    private void validateAlreadyStarted(Exercise exercise) {
        if (exercise.isAlreadyStarted()) {
            throw new ExerciseException(ExerciseErrorCode.EXERCISE_CANCEL_NOT_ALLOWED);
        }
    }

    private void validateIsJoinedExercise(Exercise exercise, Member member) {
        if (!memberExerciseRepository.existsByExerciseAndMember(exercise, member)) {
            throw new ExerciseException(ExerciseErrorCode.NOT_JOINED_EXERCISE);
        }
    }


    /**
     * 조회 메서드
     */
    private Exercise findExerciseOrThrow(Long exerciseId) {
        return exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.EXERCISE_NOT_FOUND));
    }

    private Member findMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.MEMBER_NOT_FOUND));
    }

    private Party findPartyOrThrow(Long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.PARTY_NOT_FOUND));
    }

}
