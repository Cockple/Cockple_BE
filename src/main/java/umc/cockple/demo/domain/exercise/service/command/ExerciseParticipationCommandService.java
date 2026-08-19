package umc.cockple.demo.domain.exercise.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.enums.ExerciseMemberShipStatus;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.exercise.service.ExerciseValidator;
import umc.cockple.demo.domain.exercise.service.ExerciseGameAssignmentValidator;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseCancelByManagerCommand;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseCancelResult;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseJoinResult;
import umc.cockple.demo.domain.exercise.service.support.reader.MemberExerciseReader;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;
import umc.cockple.demo.domain.exercise.service.support.reader.GuestReader;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.exercise.domain.MemberExercise;
import umc.cockple.demo.domain.exercise.events.ExerciseAttendanceChangedEvent;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.game.events.GameBoardMembersChangedEvent;
import umc.cockple.demo.domain.member.enums.MemberPartyStatus;
import umc.cockple.demo.domain.member.service.query.lookup.MemberLookupService;
import umc.cockple.demo.domain.member.service.query.lookup.MemberPartyLookupService;
import umc.cockple.demo.global.enums.Role;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ExerciseParticipationCommandService {

    private static final String MEMBER_EXERCISE_UNIQUE_CONSTRAINT =
            "uk_member_exercise_exercise_member";
    private static final String GAME_BOARD_MEMBER_UNIQUE_CONSTRAINT =
            "uk_game_board_member_board_member";

    private final MemberExerciseRepository memberExerciseRepository;
    private final GuestRepository guestRepository;
    private final ExerciseReader exerciseReader;
    private final GuestReader guestReader;
    private final MemberExerciseReader memberExerciseReader;
    private final MemberLookupService memberLookupService;
    private final MemberPartyLookupService memberPartyLookupService;
    private final ApplicationEventPublisher eventPublisher;

    private final ExerciseValidator exerciseValidator;
    private final ExerciseGameAssignmentValidator exerciseGameAssignmentValidator;

    public ExerciseJoinResult joinExercise(Long exerciseId, Long memberId) {
        log.info("운동 신청 시작 - exerciseId: {}, memberId: {}", exerciseId, memberId);

        Exercise exercise = exerciseReader.findByIdWithPartyLevelsOrThrow(exerciseId);
        Member member = memberLookupService.findByIdOrThrow(memberId);

        exerciseValidator.validateJoinExercise(exercise, member);

        boolean isPartyMember = memberPartyLookupService.isPartyMember(exercise.getParty(), member);
        ExerciseMemberShipStatus membershipStatus = isPartyMember
                ? ExerciseMemberShipStatus.PARTY_MEMBER
                : ExerciseMemberShipStatus.EXTERNAL_PARTICIPANT;
        MemberExercise memberExercise = exercise.addParticipation(member, membershipStatus);

        MemberExercise savedMemberExercise = saveParticipation(memberExercise);
        publishAttendanceChangedEvent(exercise, member.getId());
        publishGameBoardMembersChanged(exercise, memberId);

        log.info("운동 신청 종료 - memberExerciseId: {}, isPartyMember : {}"
                , savedMemberExercise.getId(), isPartyMember);

        return new ExerciseJoinResult(
                savedMemberExercise.getId(), savedMemberExercise.getCreatedAt(), exercise.getNowCapacity());
    }

    private MemberExercise saveParticipation(MemberExercise memberExercise) {
        try {
            return memberExerciseRepository.saveAndFlush(memberExercise);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateParticipation(exception)) {
                throw new ExerciseException(ExerciseErrorCode.ALREADY_JOINED_EXERCISE);
            }
            throw exception;
        }
    }

    private boolean isDuplicateParticipation(Throwable exception) {
        for (Throwable cause = exception;
             cause != null && cause != cause.getCause();
             cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && isParticipationConstraint(constraintViolation.getConstraintName())) {
                return true;
            }

            if (isParticipationConstraint(cause.getMessage())) {
                return true;
            }
        }
        return false;
    }

    private boolean isParticipationConstraint(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.toLowerCase();
        return normalized.contains(MEMBER_EXERCISE_UNIQUE_CONSTRAINT)
                || normalized.contains(GAME_BOARD_MEMBER_UNIQUE_CONSTRAINT);
    }

    public ExerciseCancelResult cancelParticipation(Long exerciseId, Long memberId) {
        log.info("운동 참여 취소 시작 - exerciseId: {}, memberId: {}", exerciseId, memberId);

        Exercise exercise = exerciseReader.findByIdOrThrow(exerciseId);
        Member member = memberLookupService.findByIdOrThrow(memberId);
        MemberExercise memberExercise = memberExerciseReader.findMemberExerciseOrThrow(exercise, member);
        exerciseValidator.validateCancelParticipation(exercise);
        exerciseGameAssignmentValidator.validateMemberCancellation(
                exercise.getGameBoard().getId(), member.getId());

        exercise.removeParticipation(memberExercise);

        memberExerciseRepository.delete(memberExercise);
        publishAttendanceChangedEvent(exercise, member.getId());
        publishGameBoardMembersChanged(exercise, memberId);

        log.info("운동 참여 취소 완료 - exerciseId: {}, memberId: {}, 현재 참여자 수: {}",
                exercise.getId(), member.getId(), exercise.getNowCapacity());

        return new ExerciseCancelResult(member.getMemberName(), exercise.getNowCapacity());
    }

    public ExerciseCancelResult cancelParticipationByManager(
            Long exerciseId, Long participantId, Long managerId, ExerciseCancelByManagerCommand command) {
        log.info("매니저에 의한 운동 참여 취소 시작 - exerciseId: {}, participantId: {}, memberId: {}",
                exerciseId, participantId, managerId);

        Exercise exercise = exerciseReader.findByIdOrThrow(exerciseId);
        Member manager = memberLookupService.findByIdOrThrow(managerId);

        exerciseValidator.validateCancelCommonParticipationByManager(exercise, manager);

        ExerciseCancelResult result = executeParticipantCancellation(exercise, participantId, command);
        publishGameBoardMembersChanged(exercise, managerId);

        log.info("매니저에 의한 운동 참여 취소 완료 - exerciseId: {}, participantId: {}, 현재 참여자 수: {}",
                exercise.getId(), participantId, exercise.getNowCapacity());

        return result;
    }

    // ========== 비즈니스 메서드 ============

    private ExerciseCancelResult executeParticipantCancellation(
            Exercise exercise, Long participantId, ExerciseCancelByManagerCommand command) {
        if(command.guest()){
            log.info("게스트 참여 취소 실행 - participantId: {}", participantId);
            return cancelGuestParticipation(exercise, participantId);
        }

        log.info("멤버 참여 취소 실행 - participantId: {}", participantId);
        return cancelMemberParticipation(exercise, participantId);
    }

    private ExerciseCancelResult cancelGuestParticipation(Exercise exercise, Long participantId) {
        Guest guest = guestReader.findByIdOrThrow(participantId);
        exerciseValidator.validateCancelGuestParticipationByManager(guest, exercise);
        exerciseGameAssignmentValidator.validateGuestCancellation(
                exercise.getGameBoard().getId(), guest.getId());

        exercise.removeGuest(guest);

        guestRepository.delete(guest);

        return new ExerciseCancelResult(guest.getGuestName(), exercise.getNowCapacity());
    }

    private ExerciseCancelResult cancelMemberParticipation(Exercise exercise, Long participantId) {
        Member participant = memberLookupService.findByIdOrThrow(participantId);
        MemberExercise memberExercise = memberExerciseReader.findMemberExerciseOrThrow(exercise, participant);
        exerciseGameAssignmentValidator.validateMemberCancellation(
                exercise.getGameBoard().getId(), participant.getId());

        exercise.removeParticipation(memberExercise);

        memberExerciseRepository.delete(memberExercise);
        publishAttendanceChangedEvent(exercise, participant.getId());

        return new ExerciseCancelResult(participant.getMemberName(), exercise.getNowCapacity());
    }

    private void publishAttendanceChangedEvent(Exercise exercise, Long subjectMemberId) {
        var party = exercise.getParty();
        List<Long> recipientMemberIds = party.getMemberParties().stream()
                .filter(memberParty -> memberParty.getStatus() == MemberPartyStatus.ACTIVE)
                .filter(memberParty -> memberParty.getRole() == Role.PARTY_MANAGER
                        || memberParty.getRole() == Role.PARTY_SUBMANAGER)
                .map(memberParty -> memberParty.getMember().getId())
                .filter(memberId -> !memberId.equals(subjectMemberId))
                .toList();

        eventPublisher.publishEvent(ExerciseAttendanceChangedEvent.changed(
                exercise.getId(),
                party.getId(),
                party.getPartyName(),
                party.getPartyImg() != null ? party.getPartyImg().getImgKey() : null,
                exercise.getDate(),
                subjectMemberId,
                recipientMemberIds
        ));
    }

    private void publishGameBoardMembersChanged(Exercise exercise, Long actorMemberId) {
        eventPublisher.publishEvent(GameBoardMembersChangedEvent.membersOnly(
                exercise.getGameBoard().getId(), actorMemberId));
    }
}
