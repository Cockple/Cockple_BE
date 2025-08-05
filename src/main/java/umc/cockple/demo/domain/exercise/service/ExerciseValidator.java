package umc.cockple.demo.domain.exercise.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.ExerciseCreateDTO;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.enums.PartyStatus;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.global.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExerciseValidator {

    private final MemberPartyRepository memberPartyRepository;
    private final MemberExerciseRepository memberExerciseRepository;

    public void validateCreateExercise(Long memberId, ExerciseCreateDTO.Request request, Party party) {
        validatePartyIsActive(party);
        validateMemberPermission(memberId, party);
        validateExerciseTime(request);
    }

    public void validateJoinExercise(Exercise exercise, Member member) {
        validateAlreadyStarted(exercise, ExerciseErrorCode.EXERCISE_ALREADY_STARTED_PARTICIPATION);
        validateAlreadyJoined(exercise, member);
        validateJoinPermission(exercise, member);
        validateMemberLevel(exercise.getParty(), member);
        validateMemberAge(exercise.getParty(), member);
    }

    public void validateGuestInvitation(Exercise exercise, Member inviter) {
        validateAlreadyStarted(exercise, ExerciseErrorCode.EXERCISE_ALREADY_STARTED_INVITATION);
        validateInviterIsPartyMember(exercise, inviter);
        validateGuestPolicy(exercise);
    }

    public void validateCancelParticipation(Exercise exercise) {
        validateAlreadyStarted(exercise, ExerciseErrorCode.EXERCISE_ALREADY_STARTED_CANCEL);
    }

    public void validateCancelGuestInvitation(Exercise exercise, Guest guest, Member member) {
        validateAlreadyStarted(exercise, ExerciseErrorCode.EXERCISE_ALREADY_STARTED_CANCEL);
        validateGuestBelongsToExercise(guest, exercise);
        validateGuestInvitedByMember(guest, member);
    }

    // ========== 세부 검증 메서드들 ==========

    private void validatePartyIsActive(Party party) {
        if (party.getStatus() == PartyStatus.INACTIVE) {
            throw new PartyException(PartyErrorCode.PARTY_IS_DELETED);
        }
    }

    private void validateMemberPermission(Long memberId, Party party) {
        boolean isOwner = party.getOwnerId().equals(memberId);
        boolean isManager = memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                party.getId(), memberId, Role.party_MANAGER);

        if (!isOwner && !isManager)
            throw new ExerciseException(ExerciseErrorCode.INSUFFICIENT_PERMISSION);
    }

    private void validateExerciseTime(ExerciseCreateDTO.Request request) {
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

    private boolean isPartyMember(Exercise exercise, Member member) {
        Party party = exercise.getParty();
        return memberPartyRepository.existsByPartyAndMember(party, member);
    }

}
