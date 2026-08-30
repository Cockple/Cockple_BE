package umc.cockple.demo.domain.exercise.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.exercise.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseCreateCommand;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseUpdateCommand;
import umc.cockple.demo.domain.member.service.query.lookup.MemberPartyLookupService;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.enums.PartyStatus;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.global.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExerciseValidator {

    private static final Set<Role> GAME_HOST_MANAGEMENT_ROLES = Set.of(
            Role.PARTY_MANAGER, Role.PARTY_SUBMANAGER);

    private final MemberPartyLookupService memberPartyLookupService;
    private final MemberExerciseRepository memberExerciseRepository;

    public void validateCreateExercise(Long memberId, ExerciseCreateCommand command, Party party) {
        validatePartyIsActive(party);
        validateSubManagerPermission(memberId, party);
        validateExerciseTime(command);
    }

    public void validateJoinExercise(Exercise exercise, Member member) {
        validateAlreadyStarted(exercise, ExerciseErrorCode.EXERCISE_ALREADY_STARTED_PARTICIPATION);
        validateAlreadyJoined(exercise, member);
        validateJoinPermission(exercise, member);
//        validateMemberLevel(exercise.getParty(), member);
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

    public void validateCancelCommonParticipationByManager(Exercise exercise, Member manager) {
        validateAlreadyStarted(exercise, ExerciseErrorCode.EXERCISE_ALREADY_STARTED_CANCEL);
        validateSubManagerPermission(manager.getId(), exercise.getParty());
    }

    public void validateCancelGuestParticipationByManager(Guest guest, Exercise exercise){
        validateGuestBelongsToExercise(guest, exercise);
    }

    public void validateDeleteExercise(Exercise exercise, Long memberId) {
        validateSubManagerPermission(memberId, exercise.getParty());
    }

    public void validateUpdateExercise(Exercise exercise, Member member, ExerciseUpdateCommand command) {
        validateSubManagerPermission(member.getId(), exercise.getParty());
        validateAlreadyStarted(exercise, ExerciseErrorCode.EXERCISE_ALREADY_STARTED_UPDATE);
        validateUpdateTime(command, exercise);
    }

    public void validateExerciseManagementPermission(Exercise exercise, Long memberId) {
        validateSubManagerPermission(memberId, exercise.getParty());
    }

    public void validateGameHostManagementPermission(Exercise exercise, Long memberId) {
        Party party = exercise.getParty();
        if (!memberPartyLookupService.hasAnyActiveRole(
                party.getId(), memberId, GAME_HOST_MANAGEMENT_ROLES)) {
            throw new ExerciseException(ExerciseErrorCode.GAME_HOST_MANAGEMENT_PERMISSION_DENIED);
        }
    }

    // ========== 세부 검증 메서드들 ==========

    private void validatePartyIsActive(Party party) {
        if (party.getStatus() == PartyStatus.INACTIVE) {
            throw new PartyException(PartyErrorCode.PARTY_IS_DELETED);
        }
    }

    private void validateSubManagerPermission(Long memberId, Party party) {
        boolean isOwner = party.getOwnerId().equals(memberId);
        boolean isManager = memberPartyLookupService.hasRole(
                party.getId(), memberId, Role.PARTY_MANAGER);
        boolean isSubManager = memberPartyLookupService.hasRole(
                party.getId(), memberId, Role.PARTY_SUBMANAGER);

        if (!isOwner && !isManager && !isSubManager)
            throw new ExerciseException(ExerciseErrorCode.INSUFFICIENT_PERMISSION);
    }

    private void validateExerciseTime(ExerciseCreateCommand command) {
        LocalDate date = command.date();
        LocalTime startTime = command.startTime();
        LocalTime endTime = command.endTime();

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
        boolean isPartyMember = memberPartyLookupService.isPartyMember(party, inviter);

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

    private void validateUpdateTime(ExerciseUpdateCommand command, Exercise exercise) {
        LocalTime newStartTime = command.startTime();
        LocalTime newEndTime = command.endTime();
        LocalDate newDate = command.date();

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

    private boolean isPartyMember(Exercise exercise, Member member) {
        Party party = exercise.getParty();
        return memberPartyLookupService.isPartyMember(party, member);
    }

}
